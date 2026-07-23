package com.yanerdan.venueflow.notification.consumer.messaging;

public enum TransferOutcome {
  CONFIRMED,
  UNROUTABLE,
  CONFIRM_NACK,
  CONFIRM_TIMEOUT,
  INTERRUPTED,
  BROKER_UNAVAILABLE
}
