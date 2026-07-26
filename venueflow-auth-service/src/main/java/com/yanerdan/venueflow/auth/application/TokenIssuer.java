package com.yanerdan.venueflow.auth.application;

import java.time.Instant;
import java.util.UUID;

public interface TokenIssuer {

  IssuedAccessToken issue(UUID userId, String username, long tokenVersion, Instant now);

  record IssuedAccessToken(String value, long expiresInSeconds) {}
}
