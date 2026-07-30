package com.yanerdan.venueflow.auth.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.yanerdan.venueflow.auth.application.AuthResult.Tokens;
import com.yanerdan.venueflow.auth.domain.ExternalIdentityBinding;
import com.yanerdan.venueflow.auth.domain.ExternalLoginCompletion;
import com.yanerdan.venueflow.auth.domain.OidcLoginTransaction;
import com.yanerdan.venueflow.auth.domain.PasswordPolicy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Profile("persistence")
public class FederatedIdentityService {

  private static final int RANDOM_BYTES = 32;
  private final FederatedIdentityRepository repository;
  private final AuthService authService;
  private final PasswordPolicy passwordPolicy;
  private final OidcProviderProperties properties;
  private final Clock clock;
  private final RestClient restClient;
  private final JwtDecoder jwtDecoder;
  private final SecureRandom random = new SecureRandom();

  public FederatedIdentityService(
      FederatedIdentityRepository repository,
      AuthService authService,
      PasswordPolicy passwordPolicy,
      OidcProviderProperties properties,
      Clock clock,
      RestClient.Builder restClientBuilder) {
    this.repository = repository;
    this.authService = authService;
    this.passwordPolicy = passwordPolicy;
    this.properties = properties;
    this.clock = clock;
    this.restClient = restClientBuilder.build();
    this.jwtDecoder = configuredDecoder(properties);
  }

  public ProviderReadiness provider() {
    List<String> missing = new ArrayList<>();
    require(missing, properties.getIssuerUri(), "issuer");
    require(missing, properties.getAuthorizationUri(), "authorization endpoint");
    require(missing, properties.getTokenUri(), "token endpoint");
    require(missing, properties.getJwkSetUri(), "JWK endpoint");
    require(missing, properties.getClientId(), "client ID");
    require(missing, properties.getClientSecret(), "client secret");
    require(missing, properties.getCallbackUri(), "callback URI");
    require(missing, properties.getBrowserReturnUri(), "browser return URI");
    boolean ready = properties.isEnabled() && missing.isEmpty() && secureProviderUris();
    String reason =
        !properties.isEnabled()
            ? "未启用"
            : !missing.isEmpty()
                ? "配置不完整：" + String.join("、", missing)
                : !secureProviderUris() ? "端点必须使用 HTTPS" : "已就绪";
    return new ProviderReadiness(
        properties.getProviderKey(),
        properties.getDisplayName(),
        properties.isEnabled(),
        ready,
        reason);
  }

  public AuthorizationStart initiate(String providerKey) {
    ProviderReadiness readiness = provider();
    if (!readiness.ready() || !readiness.key().equals(providerKey)) {
      throw unavailable();
    }
    String state = randomValue();
    String nonce = randomValue();
    String verifier = randomValue();
    LocalDateTime now = now();
    repository.createTransaction(
        hash(state),
        providerKey,
        nonce,
        verifier,
        properties.getCallbackUri(),
        now.plusMinutes(5),
        now);
    String authorizationUrl =
        UriComponentsBuilder.fromUriString(properties.getAuthorizationUri())
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.getClientId())
            .queryParam("redirect_uri", properties.getCallbackUri())
            .queryParam("scope", properties.getScopes())
            .queryParam("state", state)
            .queryParam("nonce", nonce)
            .queryParam("code_challenge", challenge(verifier))
            .queryParam("code_challenge_method", "S256")
            .build()
            .encode()
            .toUriString();
    return new AuthorizationStart(providerKey, authorizationUrl, now.plusMinutes(5));
  }

  @Transactional
  public String complete(String providerKey, String state, String authorizationCode) {
    ProviderReadiness readiness = provider();
    if (!readiness.ready() || !readiness.key().equals(providerKey)) {
      throw unavailable();
    }
    LocalDateTime now = now();
    OidcLoginTransaction transaction =
        repository
            .consumeTransaction(hash(state), providerKey, now)
            .orElseThrow(this::invalidExternalLogin);
    JsonNode tokenResponse = exchange(authorizationCode, transaction);
    String idToken = tokenResponse.path("id_token").asText("");
    if (idToken.isBlank() || jwtDecoder == null) {
      throw invalidExternalLogin();
    }
    Jwt identity = jwtDecoder.decode(idToken);
    if (!transaction.nonce().equals(identity.getClaimAsString("nonce"))) {
      throw invalidExternalLogin();
    }
    String subject = identity.getSubject();
    String usernameClaim = claim(identity, properties.getUsernameClaim());
    String campusId = claim(identity, properties.getCampusIdClaim());
    if (subject == null || subject.isBlank() || usernameClaim == null || usernameClaim.isBlank()) {
      throw invalidExternalLogin();
    }
    ExternalIdentityBinding binding =
        repository
            .findBinding(properties.getIssuerUri(), subject)
            .orElseGet(() -> provision(subject, usernameClaim, campusId, now));
    repository.touchBinding(binding.issuer(), binding.subject(), now);
    String completion = randomValue();
    repository.createCompletion(hash(completion), binding.userId(), now.plusMinutes(2), now);
    return completion;
  }

  @Transactional
  public Tokens exchangeCompletion(String completionCode) {
    LocalDateTime now = now();
    ExternalLoginCompletion completion =
        repository
            .consumeCompletion(hash(completionCode), now)
            .orElseThrow(this::invalidExternalLogin);
    return authService.federatedLogin(completion.userId());
  }

  public String browserReturn(String completionCode) {
    return UriComponentsBuilder.fromUriString(properties.getBrowserReturnUri())
        .queryParam("sso_code", completionCode)
        .build()
        .encode()
        .toUriString();
  }

  private ExternalIdentityBinding provision(
      String subject, String usernameClaim, String campusId, LocalDateTime now) {
    String normalized = externalUsername(usernameClaim);
    try {
      return repository.createExternalAccount(
          properties.getProviderKey(),
          properties.getIssuerUri(),
          subject,
          UUID.randomUUID(),
          normalized,
          blankToNull(campusId),
          now);
    } catch (DataIntegrityViolationException exception) {
      throw new AuthException(
          AuthErrorCode.AUTH_EXTERNAL_LOGIN_INVALID,
          "Campus identity conflicts with an existing account",
          exception);
    }
  }

  private String externalUsername(String value) {
    String candidate =
        (properties.getProviderKey() + "." + value)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9._-]", "-");
    if (candidate.length() > 64) {
      candidate = candidate.substring(0, 64);
    }
    return passwordPolicy.normalizeUsername(candidate);
  }

  private JsonNode exchange(String authorizationCode, OidcLoginTransaction transaction) {
    if (authorizationCode == null
        || authorizationCode.isBlank()
        || authorizationCode.length() > 2048) {
      throw invalidExternalLogin();
    }
    LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", authorizationCode);
    form.add("redirect_uri", transaction.redirectUri());
    form.add("client_id", properties.getClientId());
    form.add("client_secret", properties.getClientSecret());
    form.add("code_verifier", transaction.codeVerifier());
    try {
      JsonNode response =
          restClient
              .post()
              .uri(properties.getTokenUri())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(JsonNode.class);
      if (response == null) {
        throw invalidExternalLogin();
      }
      return response;
    } catch (AuthException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new AuthException(
          AuthErrorCode.AUTH_EXTERNAL_LOGIN_INVALID,
          "Campus identity response is invalid",
          exception);
    }
  }

  private boolean secureProviderUris() {
    return secure(properties.getIssuerUri())
        && secure(properties.getAuthorizationUri())
        && secure(properties.getTokenUri())
        && secure(properties.getJwkSetUri())
        && secure(properties.getCallbackUri())
        && secure(properties.getBrowserReturnUri());
  }

  private static boolean secure(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    URI uri = URI.create(value);
    return "https".equalsIgnoreCase(uri.getScheme())
        || ("http".equalsIgnoreCase(uri.getScheme())
            && ("127.0.0.1".equals(uri.getHost()) || "localhost".equalsIgnoreCase(uri.getHost())));
  }

  private static JwtDecoder configuredDecoder(OidcProviderProperties properties) {
    if (!properties.isEnabled()
        || properties.getJwkSetUri() == null
        || properties.getJwkSetUri().isBlank()
        || properties.getIssuerUri() == null
        || properties.getIssuerUri().isBlank()
        || properties.getClientId() == null
        || properties.getClientId().isBlank()) {
      return null;
    }
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
    OAuth2TokenValidator<Jwt> audience =
        token ->
            token.getAudience().contains(properties.getClientId())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "OIDC audience is invalid", null));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(properties.getIssuerUri()), audience));
    return decoder;
  }

  private static String claim(Jwt token, String name) {
    Object value = name == null || name.isBlank() ? null : token.getClaims().get(name);
    return value == null ? null : String.valueOf(value);
  }

  private static void require(List<String> missing, String value, String name) {
    if (value == null || value.isBlank()) {
      missing.add(name);
    }
  }

  private String randomValue() {
    byte[] bytes = new byte[RANDOM_BYTES];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String challenge(String verifier) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(digest(verifier.getBytes(StandardCharsets.US_ASCII)));
  }

  static String hash(String value) {
    if (value == null || value.length() < 16 || value.length() > 2048) {
      throw new AuthException(
          AuthErrorCode.AUTH_EXTERNAL_LOGIN_INVALID, "Campus identity response is invalid");
    }
    return HexFormat.of().formatHex(digest(value.getBytes(StandardCharsets.US_ASCII)));
  }

  private static byte[] digest(byte[] value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }

  private AuthException unavailable() {
    return new AuthException(
        AuthErrorCode.AUTH_PROVIDER_UNAVAILABLE, "Campus identity provider is unavailable");
  }

  private AuthException invalidExternalLogin() {
    return new AuthException(
        AuthErrorCode.AUTH_EXTERNAL_LOGIN_INVALID, "Campus identity response is invalid");
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public record ProviderReadiness(
      String key, String displayName, boolean enabled, boolean ready, String status) {}

  public record AuthorizationStart(
      String providerKey, String authorizationUrl, LocalDateTime expiresAt) {}
}
