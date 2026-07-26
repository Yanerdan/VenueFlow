package com.yanerdan.venueflow.booking.reconciliation.domain;

public record ReconciliationIntentId(long value) {

  public ReconciliationIntentId {
    if (value <= 0) {
      throw new IllegalArgumentException("Reconciliation intent id must be positive");
    }
  }
}
