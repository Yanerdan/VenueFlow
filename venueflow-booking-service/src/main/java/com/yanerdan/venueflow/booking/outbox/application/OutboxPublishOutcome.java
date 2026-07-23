package com.yanerdan.venueflow.booking.outbox.application;

public enum OutboxPublishOutcome {
  CONFIRMED,
  UNROUTABLE,
  CONFIRM_NACK,
  CONFIRM_TIMEOUT,
  BROKER_UNAVAILABLE,
  PUBLISH_INTERRUPTED
}
