package com.yanerdan.venueflow.auth.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthCredential(
    long id,
    UUID userId,
    String username,
    String passwordHash,
    CampusRole role,
    int failedAttempts,
    LocalDateTime lockedUntil,
    long tokenVersion,
    long version) {

  public boolean lockedAt(LocalDateTime now) {
    return lockedUntil != null && now.isBefore(lockedUntil);
  }
}
