package com.yanerdan.venueflow.booking.reconciliation.domain;

import java.time.LocalDateTime;

public record ReconciliationIntent(
    long id,
    ReconciliationWorkflowType workflowType,
    String requestId,
    Long bookingId,
    long slotId,
    int quantity,
    String allocationOperationId,
    String releaseOperationId,
    ReconciliationIntentState state,
    int attemptCount,
    long version,
    String leaseOwner,
    LocalDateTime nextCheckAt,
    LocalDateTime createdAt) {}
