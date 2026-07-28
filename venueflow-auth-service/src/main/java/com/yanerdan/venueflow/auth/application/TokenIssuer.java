package com.yanerdan.venueflow.auth.application;

import com.yanerdan.venueflow.auth.domain.CampusRole;
import java.time.Instant;
import java.util.UUID;

public interface TokenIssuer {

  IssuedAccessToken issue(
      UUID userId, String username, CampusRole role, long tokenVersion, Instant now);

  record IssuedAccessToken(String value, long expiresInSeconds) {}
}
