package com.yanerdan.venueflow.auth.application;

import com.yanerdan.venueflow.auth.application.AuthResult.Identity;
import com.yanerdan.venueflow.auth.application.AuthResult.Tokens;
import com.yanerdan.venueflow.auth.domain.AuthCredential;
import com.yanerdan.venueflow.auth.domain.PasswordPolicy;
import com.yanerdan.venueflow.auth.domain.RefreshSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("persistence")
public class AuthService {

  private static final String INVALID_CREDENTIALS = "Credentials are invalid";
  private static final String INVALID_REFRESH = "Refresh token is invalid";

  private final AuthRepository repository;
  private final PasswordPolicy passwordPolicy;
  private final PasswordEncoder passwordEncoder;
  private final TokenIssuer tokenIssuer;
  private final Clock clock;
  private final Duration refreshTtl;
  private final int maxLoginAttempts;
  private final Duration lockoutDuration;
  private final boolean localLoginEnabled;
  private final String dummyHash;
  private final SecureRandom random = new SecureRandom();

  public AuthService(
      AuthRepository repository,
      PasswordPolicy passwordPolicy,
      PasswordEncoder passwordEncoder,
      TokenIssuer tokenIssuer,
      Clock clock,
      @Value("${venueflow.auth.refresh-token-ttl}") Duration refreshTtl,
      @Value("${venueflow.auth.max-login-attempts}") int maxLoginAttempts,
      @Value("${venueflow.auth.lockout-duration}") Duration lockoutDuration,
      @Value("${venueflow.auth.local-login-enabled:true}") boolean localLoginEnabled) {
    this.repository = repository;
    this.passwordPolicy = passwordPolicy;
    this.passwordEncoder = passwordEncoder;
    this.tokenIssuer = tokenIssuer;
    this.clock = clock;
    this.refreshTtl =
        between(refreshTtl, Duration.ofHours(1), Duration.ofDays(30), "refresh token TTL");
    if (maxLoginAttempts < 1 || maxLoginAttempts > 20) {
      throw new IllegalArgumentException("max login attempts is outside its allowed range");
    }
    this.maxLoginAttempts = maxLoginAttempts;
    this.lockoutDuration =
        between(lockoutDuration, Duration.ofMinutes(1), Duration.ofHours(24), "lockout duration");
    this.localLoginEnabled = localLoginEnabled;
    this.dummyHash = passwordEncoder.encode("VenueFlow-Dummy-Password-2026");
  }

  @Transactional
  public Identity register(String username, char[] password) {
    String normalized = passwordPolicy.normalizeUsername(username);
    passwordPolicy.validatePassword(password);
    UUID userId = UUID.randomUUID();
    try {
      repository.createCredential(
          userId, normalized, passwordEncoder.encode(new String(password)), nowLocal());
      return new Identity(userId, normalized);
    } catch (DataIntegrityViolationException exception) {
      throw new AuthException(
          AuthErrorCode.AUTH_USERNAME_EXISTS, "Username already exists", exception);
    } finally {
      wipe(password);
    }
  }

  @Transactional
  public Tokens login(String username, char[] password) {
    if (!localLoginEnabled) {
      passwordEncoder.matches(new String(password), dummyHash);
      wipe(password);
      throw invalidCredentials();
    }
    String normalized;
    try {
      normalized = passwordPolicy.normalizeUsername(username);
    } catch (IllegalArgumentException exception) {
      passwordEncoder.matches(new String(password), dummyHash);
      wipe(password);
      throw invalidCredentials();
    }

    AuthCredential credential = repository.findCredential(normalized).orElse(null);
    String hash =
        credential == null || credential.passwordHash() == null
            ? dummyHash
            : credential.passwordHash();
    boolean matches;
    try {
      matches = passwordEncoder.matches(new String(password), hash);
    } finally {
      wipe(password);
    }
    LocalDateTime now = nowLocal();
    if (credential == null || credential.passwordHash() == null) {
      throw invalidCredentials();
    }
    if (credential.lockedAt(now)) {
      throw invalidCredentials();
    }
    if (!matches) {
      int attempts = credential.failedAttempts() + 1;
      LocalDateTime lockedUntil = attempts >= maxLoginAttempts ? now.plus(lockoutDuration) : null;
      repository.recordFailure(credential.id(), credential.version(), attempts, lockedUntil, now);
      throw invalidCredentials();
    }
    if (credential.failedAttempts() != 0 || credential.lockedUntil() != null) {
      repository.resetFailures(credential.id(), credential.version(), now);
    }
    return issuePair(
        credential.userId(), credential.username(), credential.role(), credential.tokenVersion());
  }

  @Transactional
  public Tokens refresh(String refreshToken) {
    String oldHash = hash(refreshToken);
    RefreshSession session = repository.lockRefresh(oldHash).orElseThrow(this::invalidRefresh);
    LocalDateTime now = nowLocal();
    if (!session.activeAt(now)) {
      throw invalidRefresh();
    }
    AuthCredential credential =
        repository.findCredential(session.userId()).orElseThrow(this::invalidRefresh);
    if (credential.tokenVersion() != session.tokenVersion()) {
      throw invalidRefresh();
    }
    String replacement = randomToken();
    String replacementHash = hash(replacement);
    if (!repository.revokeRefresh(oldHash, replacementHash, now)) {
      throw invalidRefresh();
    }
    repository.createRefresh(
        replacementHash,
        session.userId(),
        session.username(),
        session.tokenVersion(),
        now.plus(refreshTtl),
        now);
    TokenIssuer.IssuedAccessToken access =
        tokenIssuer.issue(
            session.userId(),
            session.username(),
            credential.role(),
            session.tokenVersion(),
            clock.instant());
    return new Tokens(access.value(), replacement, "Bearer", access.expiresInSeconds());
  }

  @Transactional
  public void logout(String refreshToken) {
    repository.revokeRefresh(hash(refreshToken), nowLocal());
  }

  @Transactional
  public Tokens federatedLogin(UUID userId) {
    AuthCredential credential =
        repository.findCredential(userId).orElseThrow(this::invalidCredentials);
    return issuePair(
        credential.userId(), credential.username(), credential.role(), credential.tokenVersion());
  }

  private Tokens issuePair(
      UUID userId,
      String username,
      com.yanerdan.venueflow.auth.domain.CampusRole role,
      long tokenVersion) {
    Instant now = clock.instant();
    String refresh = randomToken();
    repository.createRefresh(
        hash(refresh),
        userId,
        username,
        tokenVersion,
        LocalDateTime.ofInstant(now.plus(refreshTtl), ZoneOffset.UTC),
        LocalDateTime.ofInstant(now, ZoneOffset.UTC));
    TokenIssuer.IssuedAccessToken access =
        tokenIssuer.issue(userId, username, role, tokenVersion, now);
    return new Tokens(access.value(), refresh, "Bearer", access.expiresInSeconds());
  }

  private String randomToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String hash(String token) {
    if (token == null || token.length() < 32 || token.length() > 128) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN, INVALID_REFRESH);
    }
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(token.getBytes(StandardCharsets.US_ASCII)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private LocalDateTime nowLocal() {
    return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }

  private AuthException invalidCredentials() {
    return new AuthException(AuthErrorCode.AUTH_INVALID_CREDENTIALS, INVALID_CREDENTIALS);
  }

  private AuthException invalidRefresh() {
    return new AuthException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN, INVALID_REFRESH);
  }

  private static Duration between(Duration value, Duration minimum, Duration maximum, String name) {
    if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
      throw new IllegalArgumentException(name + " is outside its allowed range");
    }
    return value;
  }

  private static void wipe(char[] password) {
    if (password != null) {
      Arrays.fill(password, '\0');
    }
  }
}
