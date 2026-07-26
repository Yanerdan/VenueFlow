package com.yanerdan.venueflow.booking.reconciliation.application.port;

import com.yanerdan.venueflow.booking.reconciliation.domain.NewReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntentId;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationOutcomeCode;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationWorkflowType;
import java.time.LocalDateTime;
import java.util.List;

public interface ReconciliationIntentRepository {

  ReconciliationIntentId create(NewReconciliationIntent intent);

  boolean resolveOpen(
      ReconciliationWorkflowType workflowType,
      String requestId,
      Long bookingId,
      ReconciliationOutcomeCode outcomeCode,
      LocalDateTime resolvedAt);

  List<ReconciliationIntent> previewDue(LocalDateTime now, int limit);

  ClaimedIntents claimDue(
      LocalDateTime now, int limit, String leaseOwner, LocalDateTime leaseExpiresAt);

  boolean resolve(
      long intentId,
      long expectedVersion,
      String leaseOwner,
      ReconciliationOutcomeCode outcomeCode,
      LocalDateTime resolvedAt);

  boolean scheduleRetry(
      long intentId,
      long expectedVersion,
      String leaseOwner,
      String errorCode,
      LocalDateTime nextCheckAt,
      LocalDateTime updatedAt);

  boolean exhaust(
      long intentId,
      long expectedVersion,
      String leaseOwner,
      String errorCode,
      LocalDateTime resolvedAt);

  long dueCount(LocalDateTime now);

  long oldestDueAgeSeconds(LocalDateTime now);

  record ClaimedIntents(List<ReconciliationIntent> intents, int leaseReclaimed) {
    public ClaimedIntents {
      intents = List.copyOf(intents);
    }
  }
}
