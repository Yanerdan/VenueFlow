package com.yanerdan.venueflow.auth.application;

import java.util.UUID;

public final class AuthResult {

  private AuthResult() {}

  public record Identity(UUID userId, String username) {}

  public record Tokens(
      String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {}
}
