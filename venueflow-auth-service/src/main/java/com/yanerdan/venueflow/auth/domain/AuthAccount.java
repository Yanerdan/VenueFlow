package com.yanerdan.venueflow.auth.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthAccount(
    UUID userId,
    String username,
    CampusRole role,
    long tokenVersion,
    long version,
    LocalDateTime updatedAt) {}
