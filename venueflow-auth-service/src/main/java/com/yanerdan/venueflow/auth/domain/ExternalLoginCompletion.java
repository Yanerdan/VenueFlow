package com.yanerdan.venueflow.auth.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExternalLoginCompletion(
    String codeHash, UUID userId, LocalDateTime expiresAt, LocalDateTime consumedAt) {

  public boolean activeAt(LocalDateTime now) {
    return consumedAt == null && expiresAt.isAfter(now);
  }
}
