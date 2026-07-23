package com.yanerdan.venueflow.booking.outbox.domain;

public enum OutboxStatus {
  NEW,
  PUBLISHING,
  RETRY,
  PUBLISHED,
  DEAD
}
