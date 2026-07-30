package com.yanerdan.venueflow.auth.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExternalIdentityBinding(
    String providerKey,
    String issuer,
    String subject,
    UUID userId,
    String username,
    String campusId,
    LocalDateTime lastLoginAt) {}
