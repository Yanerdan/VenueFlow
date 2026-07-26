package com.yanerdan.venueflow.booking.reconciliation.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record NewReconciliationIntent(
    ReconciliationWorkflowType workflowType,
    String requestId,
    Long bookingId,
    long slotId,
    int quantity,
    String allocationOperationId,
    String releaseOperationId,
    LocalDateTime nextCheckAt) {

  private static final int MAX_REQUEST_ID_LENGTH = 128;
  private static final int MAX_OPERATION_ID_LENGTH = 128;

  public NewReconciliationIntent {
    Objects.requireNonNull(workflowType, "Workflow type must not be null");

    requestId = requireBoundedText(requestId, MAX_REQUEST_ID_LENGTH, "Request id");

    allocationOperationId =
        requireBoundedText(
            allocationOperationId, MAX_OPERATION_ID_LENGTH, "Allocation operation id");

    releaseOperationId =
        requireBoundedText(releaseOperationId, MAX_OPERATION_ID_LENGTH, "Release operation id");

    Objects.requireNonNull(nextCheckAt, "Next check time must not be null");

    if (bookingId != null && bookingId <= 0) {
      throw new IllegalArgumentException("Booking id must be positive");
    }

    if (slotId <= 0) {
      throw new IllegalArgumentException("Slot id must be positive");
    }

    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }

    if (workflowType == ReconciliationWorkflowType.RELEASE && bookingId == null) {
      throw new IllegalArgumentException("Release intent requires a booking id");
    }
  }

  private static String requireBoundedText(String value, int maximumLength, String name) {
    Objects.requireNonNull(value, name + " must not be null");

    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }

    if (!value.equals(value.trim())) {
      throw new IllegalArgumentException(name + " must not contain surrounding whitespace");
    }

    if (value.length() > maximumLength) {
      throw new IllegalArgumentException(
          name + " must not exceed " + maximumLength + " characters");
    }

    return value;
  }
}
