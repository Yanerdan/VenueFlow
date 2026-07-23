package com.yanerdan.venueflow.booking.outbox.application;

import com.yanerdan.venueflow.booking.outbox.domain.OutboxEvent;
import com.yanerdan.venueflow.booking.outbox.domain.OutboxStatus;
import java.time.LocalDateTime;

public record OutboxEventMetadata(
    String eventId,
    String aggregateId,
    String eventType,
    int eventVersion,
    OutboxStatus status,
    int retryCount,
    LocalDateTime nextRetryAt,
    LocalDateTime leaseUntil,
    LocalDateTime createdAt,
    LocalDateTime publishedAt,
    String lastErrorCode) {
  static OutboxEventMetadata from(OutboxEvent event) {
    return new OutboxEventMetadata(
        event.eventId(),
        event.aggregateId(),
        event.eventType(),
        event.eventVersion(),
        event.status(),
        event.retryCount(),
        event.nextRetryAt(),
        event.leaseUntil(),
        event.createdAt(),
        event.publishedAt(),
        event.lastErrorCode());
  }
}
