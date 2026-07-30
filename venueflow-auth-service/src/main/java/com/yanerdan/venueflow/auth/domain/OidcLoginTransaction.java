package com.yanerdan.venueflow.auth.domain;

import java.time.LocalDateTime;

public record OidcLoginTransaction(
    String stateHash,
    String providerKey,
    String nonce,
    String codeVerifier,
    String redirectUri,
    LocalDateTime expiresAt,
    LocalDateTime consumedAt) {

  public boolean activeAt(LocalDateTime now) {
    return consumedAt == null && expiresAt.isAfter(now);
  }
}
