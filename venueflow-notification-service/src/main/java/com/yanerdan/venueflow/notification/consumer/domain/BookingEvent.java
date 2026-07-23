package com.yanerdan.venueflow.notification.consumer.domain;

import java.time.Instant;

public record BookingEvent(
    String eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    String producer,
    String aggregateType,
    String aggregateId,
    String traceId,
    String bookingNo,
    long userId,
    long slotId,
    int quantity,
    String status,
    String routingKey,
    String payloadHash) {

  public boolean sameIdentity(ConsumedIdentity identity) {
    return eventType.equals(identity.eventType())
        && eventVersion == identity.eventVersion()
        && payloadHash.equals(identity.payloadHash());
  }
}
