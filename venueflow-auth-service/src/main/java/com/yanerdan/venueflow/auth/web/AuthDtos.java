package com.yanerdan.venueflow.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuthDtos {

  private AuthDtos() {}

  public record RegisterRequest(
      @NotBlank @Size(min = 3, max = 64) String username,
      @NotNull @Size(min = 12, max = 72) String password) {}

  public record LoginRequest(
      @NotBlank @Size(min = 3, max = 64) String username,
      @NotNull @Size(min = 1, max = 72) String password) {}

  public record RefreshRequest(@NotBlank @Size(min = 32, max = 128) String refreshToken) {}

  public record IdentityResponse(UUID userId, String username) {}

  public record TokenResponse(
      String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {}

  public record SuccessResponse<T>(String code, String message, T data, String traceId) {}

  public record ErrorResponse(
      String code, String message, List<String> details, String traceId, Instant timestamp) {}
}
