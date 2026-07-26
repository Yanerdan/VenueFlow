package com.yanerdan.venueflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("gateway")
class GatewaySecurityRoutingIT {

  private static final String ISSUER = "venueflow-auth-service";
  private static final String SUBJECT = "10000000-0000-0000-0000-000000000001";
  private static final AtomicInteger CALLS = new AtomicInteger();
  private static final AtomicReference<String> USER_ID = new AtomicReference<>();
  private static final AtomicReference<String> ROLE = new AtomicReference<>();
  private static final AtomicReference<String> TRACE_ID = new AtomicReference<>();
  private static final KeyPair KEY_PAIR = keyPair();
  private static final HttpServer DOWNSTREAM = downstream();

  @Autowired private Environment environment;

  @DynamicPropertySource
  static void gatewayProperties(DynamicPropertyRegistry registry) {
    String uri = "http://127.0.0.1:" + DOWNSTREAM.getAddress().getPort();
    registry.add("venueflow.gateway.auth-uri", () -> uri);
    registry.add("venueflow.gateway.user-uri", () -> uri);
    registry.add("venueflow.gateway.resource-uri", () -> uri);
    registry.add("venueflow.gateway.booking-uri", () -> uri);
    registry.add("venueflow.gateway.issuer", () -> ISSUER);
    registry.add("venueflow.gateway.jwt-public-key", GatewaySecurityRoutingIT::publicKeyPem);
    registry.add("venueflow.gateway.allowed-origins", () -> "https://app.example.test");
    registry.add("venueflow.gateway.max-request-bytes", () -> "1024");
  }

  @BeforeEach
  void resetDownstreamEvidence() {
    CALLS.set(0);
    USER_ID.set(null);
    ROLE.set(null);
    TRACE_ID.set(null);
  }

  @AfterAll
  static void stopDownstream() {
    DOWNSTREAM.stop(0);
  }

  @Test
  void publicAuthRouteReachesOnlyConfiguredDownstream() {
    client()
        .post()
        .uri("/api/v1/auth/login")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("ok");
    assertThat(CALLS).hasValue(1);
  }

  @Test
  void missingAndWrongIssuerTokensNeverReachDownstream() throws Exception {
    client()
        .get()
        .uri("/api/v1/bookings/1")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("GATEWAY_UNAUTHORIZED");
    client()
        .get()
        .uri("/api/v1/bookings/1")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("other-issuer"))
        .exchange()
        .expectStatus()
        .isUnauthorized();
    assertThat(CALLS).hasValue(0);
  }

  @Test
  void verifiedSubjectReplacesForgedIdentityAndTraceIsPropagated() throws Exception {
    client()
        .get()
        .uri("/api/v1/users/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(ISSUER))
        .header("X-User-Id", "forged")
        .header("X-Role", "admin")
        .header("X-Trace-Id", "not-a-uuid")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueMatches("X-Trace-Id", "[0-9a-f-]{36}");

    assertThat(CALLS).hasValue(1);
    assertThat(USER_ID).hasValue(SUBJECT);
    assertThat(ROLE).hasValue(null);
    assertThat(TRACE_ID.get()).matches("[0-9a-f-]{36}");
  }

  @Test
  void declaredOversizedBodyIsRejectedBeforeRouting() {
    client()
        .post()
        .uri("/api/v1/auth/login")
        .bodyValue("x".repeat(1025))
        .exchange()
        .expectStatus()
        .isEqualTo(413)
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("GATEWAY_PAYLOAD_TOO_LARGE");
    assertThat(CALLS).hasValue(0);
  }

  @Test
  void corsAllowsOnlyExplicitOrigin() {
    client()
        .options()
        .uri("/api/v1/auth/login")
        .header(HttpHeaders.ORIGIN, "https://app.example.test")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.test");

    client()
        .options()
        .uri("/api/v1/auth/login")
        .header(HttpHeaders.ORIGIN, "https://evil.example.test")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .exchange()
        .expectHeader()
        .doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
  }

  private WebTestClient client() {
    int port = environment.getRequiredProperty("local.server.port", Integer.class);
    return WebTestClient.bindToServer()
        .baseUrl("http://127.0.0.1:" + port)
        .responseTimeout(java.time.Duration.ofSeconds(5))
        .build();
  }

  private static String token(String issuer) throws Exception {
    Instant now = Instant.now();
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader(JWSAlgorithm.RS256),
            new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(SUBJECT)
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.minusSeconds(1)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .build());
    jwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
    return jwt.serialize();
  }

  private static String publicKeyPem() {
    return "-----BEGIN PUBLIC KEY-----\n"
        + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
            .encodeToString(KEY_PAIR.getPublic().getEncoded())
        + "\n-----END PUBLIC KEY-----";
  }

  private static KeyPair keyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static HttpServer downstream() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            CALLS.incrementAndGet();
            USER_ID.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
            ROLE.set(exchange.getRequestHeaders().getFirst("X-Role"));
            TRACE_ID.set(exchange.getRequestHeaders().getFirst("X-Trace-Id"));
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
