package com.yanerdan.venueflow.auth.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("persistence")
class AuthLifecycleAuthSuite {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final KeyPair KEYS = keys();

  @Container
  static final MySQLContainer MYSQL =
      new MySQLContainer("mysql:8.4.10")
          .withDatabaseName("venueflow_auth")
          .withUsername("venueflow_auth")
          .withPassword("auth-test-password");

  @Autowired private MockMvc mvc;

  @Autowired private JdbcTemplate jdbc;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("VENUEFLOW_AUTH_DB_URL", MYSQL::getJdbcUrl);
    registry.add("VENUEFLOW_AUTH_DB_USERNAME", MYSQL::getUsername);
    registry.add("VENUEFLOW_AUTH_DB_PASSWORD", MYSQL::getPassword);
    registry.add("JWT_PRIVATE_KEY", () -> privatePem(KEYS));
    registry.add("JWT_PUBLIC_KEY", () -> publicPem(KEYS));
    registry.add("VENUEFLOW_AUTH_MAX_LOGIN_ATTEMPTS", () -> "2");
    registry.add("VENUEFLOW_AUTH_LOCKOUT_DURATION", () -> "PT1M");
  }

  @Test
  void migratesAndCompletesRegisterLoginRefreshLogoutLifecycle() throws Exception {
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '001'", Integer.class))
        .isEqualTo(1);

    JsonNode identity =
        response(
            "/api/v1/auth/register",
            """
            {"username":"Alice.Example","password":"VenueFlow2026!"}
            """,
            201);
    assertThat(identity.at("/data/username").asText()).isEqualTo("alice.example");
    assertThat(identity.at("/data/userId").asText()).isNotBlank();
    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash FROM auth_credentials WHERE username = 'alice.example'",
                String.class))
        .startsWith("$2");

    response(
        "/api/v1/auth/login",
        """
        {"username":"alice.example","password":"WrongPassword1!"}
        """,
        401);

    JsonNode login =
        response(
            "/api/v1/auth/login",
            """
            {"username":"alice.example","password":"VenueFlow2026!"}
            """,
            200);
    assertThat(
            jdbc.queryForObject(
                "SELECT failed_attempts FROM auth_credentials WHERE username = 'alice.example'",
                Integer.class))
        .isZero();

    String access = login.at("/data/accessToken").asText();
    String refresh = login.at("/data/refreshToken").asText();
    assertJwt(access, identity.at("/data/userId").asText(), "alice.example");

    JsonNode rotated =
        response("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}", 200);
    String replacement = rotated.at("/data/refreshToken").asText();
    assertThat(replacement).isNotEqualTo(refresh);
    response("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}", 401);

    response("/api/v1/auth/logout", "{\"refreshToken\":\"" + replacement + "\"}", 200);
    response("/api/v1/auth/logout", "{\"refreshToken\":\"" + replacement + "\"}", 200);
    response("/api/v1/auth/refresh", "{\"refreshToken\":\"" + replacement + "\"}", 401);
  }

  @Test
  void locksAfterBoundedFailuresWithoutRevealingUsernameExistence() throws Exception {
    response(
        "/api/v1/auth/register",
        """
        {"username":"locked.user","password":"VenueFlow2026!"}
        """,
        201);
    String wrong = "{\"username\":\"locked.user\",\"password\":\"WrongPassword1!\"}";
    assertThat(response("/api/v1/auth/login", wrong, 401).path("code").asText())
        .isEqualTo("AUTH_INVALID_CREDENTIALS");
    assertThat(response("/api/v1/auth/login", wrong, 401).path("code").asText())
        .isEqualTo("AUTH_INVALID_CREDENTIALS");
    assertThat(
            response(
                    "/api/v1/auth/login",
                    """
                    {"username":"locked.user","password":"VenueFlow2026!"}
                    """,
                    401)
                .path("code")
                .asText())
        .isEqualTo("AUTH_INVALID_CREDENTIALS");
  }

  private JsonNode response(String path, String body, int expectedStatus) throws Exception {
    String content =
        mvc.perform(
                post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(expectedStatus))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JSON.readTree(content);
  }

  private static void assertJwt(String jwt, String expectedSubject, String expectedUsername)
      throws Exception {
    String[] parts = jwt.split("\\.");
    assertThat(parts).hasSize(3);
    JsonNode claims = JSON.readTree(Base64.getUrlDecoder().decode(parts[1]));
    assertThat(claims.path("iss").asText()).isEqualTo("venueflow-auth-service");
    assertThat(claims.path("sub").asText()).isEqualTo(expectedSubject);
    assertThat(claims.path("username").asText()).isEqualTo(expectedUsername);
    assertThat(claims.path("role").asText()).isEqualTo("APPLICANT");
    assertThat(claims.path("exp").asLong() - claims.path("iat").asLong()).isEqualTo(900);
    assertThat(claims.has("password")).isFalse();

    PublicKey publicKey =
        KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(KEYS.getPublic().getEncoded()));
    Signature verifier = Signature.getInstance("SHA256withRSA");
    verifier.initVerify(publicKey);
    verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
    assertThat(verifier.verify(Base64.getUrlDecoder().decode(parts[2]))).isTrue();
  }

  private static KeyPair keys() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static String privatePem(KeyPair pair) {
    return pem("PRIVATE KEY", pair.getPrivate().getEncoded());
  }

  private static String publicPem(KeyPair pair) {
    return pem("PUBLIC KEY", pair.getPublic().getEncoded());
  }

  private static String pem(String type, byte[] bytes) {
    return "-----BEGIN "
        + type
        + "-----\n"
        + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(bytes)
        + "\n-----END "
        + type
        + "-----";
  }
}
