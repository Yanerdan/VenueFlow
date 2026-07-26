package com.yanerdan.venueflow.booking.reconciliation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NewReconciliationIntentTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 24, 12, 0);

  @Test
  void acceptsAllocationIntentWithoutBookingId() {
    NewReconciliationIntent intent =
        new NewReconciliationIntent(
            ReconciliationWorkflowType.ALLOCATE,
            "request-123",
            null,
            10L,
            2,
            "allocation-operation-123",
            "release-operation-123",
            NOW);

    assertThat(intent.workflowType()).isEqualTo(ReconciliationWorkflowType.ALLOCATE);

    assertThat(intent.bookingId()).isNull();
  }

  @Test
  void requiresBookingIdForReleaseIntent() {
    assertThatThrownBy(
            () ->
                new NewReconciliationIntent(
                    ReconciliationWorkflowType.RELEASE,
                    "request-123",
                    null,
                    10L,
                    2,
                    "allocation-operation-123",
                    "release-operation-123",
                    NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("booking id");
  }

  @Test
  void rejectsNonPositiveQuantity() {
    assertThatThrownBy(
            () ->
                new NewReconciliationIntent(
                    ReconciliationWorkflowType.ALLOCATE,
                    "request-123",
                    null,
                    10L,
                    0,
                    "allocation-operation-123",
                    "release-operation-123",
                    NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Quantity");
  }
}
