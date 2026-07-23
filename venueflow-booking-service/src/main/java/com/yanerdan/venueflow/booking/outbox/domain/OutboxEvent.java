package com.yanerdan.venueflow.booking.outbox.domain;

import java.time.LocalDateTime;

public record OutboxEvent(
    Long id,
    String eventId,
    String aggregateType,
    String aggregateId,
    String eventType,
    int eventVersion,
    String routingKey,
    String payload,
    String headers,
    OutboxStatus status,
    int retryCount,
    LocalDateTime nextRetryAt,
    String claimToken,
    LocalDateTime leaseUntil,
    LocalDateTime createdAt,
    LocalDateTime publishedAt,
    String lastErrorCode,
    long version) {}
