package com.yanerdan.venueflow.auth.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanerdan.venueflow.auth.application.TokenIssuer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence")
public class RsaJwtIssuer implements TokenIssuer {

  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

  private final PrivateKey privateKey;
  private final PublicKey publicKey;
  private final String issuer;
  private final Duration ttl;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public RsaJwtIssuer(
      @Value("${venueflow.auth.jwt-private-key}") String privatePem,
      @Value("${venueflow.auth.jwt-public-key}") String publicPem,
      @Value("${venueflow.auth.issuer}") String issuer,
      @Value("${venueflow.auth.access-token-ttl}") Duration ttl) {
    this.privateKey = parsePrivate(privatePem);
    this.publicKey = parsePublic(publicPem);
    this.issuer = issuer;
    this.ttl = requireBetween(ttl, Duration.ofMinutes(1), Duration.ofHours(1), "access token TTL");
    verifyKeyPair();
  }

  @Override
  public IssuedAccessToken issue(UUID userId, String username, long tokenVersion, Instant now) {
    Instant expiresAt = now.plus(ttl);
    Map<String, Object> header = Map.of("alg", "RS256", "typ", "JWT");
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("iss", issuer);
    claims.put("sub", userId.toString());
    claims.put("username", username);
    claims.put("ver", tokenVersion);
    claims.put("jti", UUID.randomUUID().toString());
    claims.put("iat", now.getEpochSecond());
    claims.put("exp", expiresAt.getEpochSecond());
    try {
      String unsigned = encode(header) + "." + encode(claims);
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initSign(privateKey);
      signature.update(unsigned.getBytes(StandardCharsets.US_ASCII));
      return new IssuedAccessToken(
          unsigned + "." + URL_ENCODER.encodeToString(signature.sign()), ttl.toSeconds());
    } catch (GeneralSecurityException | JsonProcessingException exception) {
      throw new IllegalStateException("Unable to issue access token", exception);
    }
  }

  private String encode(Map<String, Object> value) throws JsonProcessingException {
    return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
  }

  private void verifyKeyPair() {
    try {
      byte[] probe = "venueflow-auth-key-check".getBytes(StandardCharsets.US_ASCII);
      Signature signer = Signature.getInstance("SHA256withRSA");
      signer.initSign(privateKey);
      signer.update(probe);
      byte[] signed = signer.sign();
      Signature verifier = Signature.getInstance("SHA256withRSA");
      verifier.initVerify(publicKey);
      verifier.update(probe);
      if (!verifier.verify(signed)) {
        throw new IllegalArgumentException("JWT RSA keys do not form a pair");
      }
    } catch (GeneralSecurityException exception) {
      throw new IllegalArgumentException("Invalid JWT RSA key pair", exception);
    }
  }

  private static PrivateKey parsePrivate(String pem) {
    try {
      byte[] bytes = Base64.getDecoder().decode(body(pem, "PRIVATE KEY"));
      return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid JWT private key", exception);
    }
  }

  private static PublicKey parsePublic(String pem) {
    try {
      byte[] bytes = Base64.getDecoder().decode(body(pem, "PUBLIC KEY"));
      return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid JWT public key", exception);
    }
  }

  private static String body(String pem, String type) {
    if (pem == null || pem.isBlank()) {
      throw new IllegalArgumentException("Missing JWT " + type.toLowerCase(Locale.ROOT));
    }
    return pem.replace("\\n", "\n")
        .replace("-----BEGIN " + type + "-----", "")
        .replace("-----END " + type + "-----", "")
        .replaceAll("\\s", "");
  }

  private static Duration requireBetween(
      Duration value, Duration minimum, Duration maximum, String name) {
    if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
      throw new IllegalArgumentException(name + " is outside its allowed range");
    }
    return value;
  }
}
