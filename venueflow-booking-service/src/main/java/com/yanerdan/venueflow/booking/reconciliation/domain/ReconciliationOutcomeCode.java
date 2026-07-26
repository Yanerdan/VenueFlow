package com.yanerdan.venueflow.booking.reconciliation.domain;

public enum ReconciliationOutcomeCode {
  NO_ALLOCATION,
  ALREADY_CONSISTENT,
  ORPHAN_RELEASED,
  CANCELLATION_COMPLETED,
  ALREADY_CANCELLED,
  ATTEMPTS_EXHAUSTED
}
