package com.yanerdan.venueflow.booking.reconciliation.application;

public record ReconciliationSummary(
    int claimed, int consistent, int repaired, int unresolved, int failed, int leaseReclaimed) {}
