package com.yanerdan.venueflow.auth.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefreshSession(
    long id,
    String tokenHash,
    UUID userId,
    String username,
    long tokenVersion,
    LocalDateTime expiresAt,
    LocalDateTime revokedAt) {

  public boolean activeAt(LocalDateTime now) {
    return revokedAt == null && now.isBefore(expiresAt);
  }
}
