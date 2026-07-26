package com.yanerdan.venueflow.booking.reconciliation.persistence;

import com.yanerdan.venueflow.booking.reconciliation.application.port.ReconciliationIntentRepository;
import com.yanerdan.venueflow.booking.reconciliation.application.port.ReconciliationIntentRepository.ClaimedIntents;
import com.yanerdan.venueflow.booking.reconciliation.domain.NewReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntentId;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntentState;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationOutcomeCode;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationWorkflowType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("persistence")
public class MyBatisReconciliationIntentRepository implements ReconciliationIntentRepository {
  private final ReconciliationIntentMapper mapper;

  public MyBatisReconciliationIntentRepository(ReconciliationIntentMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public ReconciliationIntentId create(NewReconciliationIntent intent) {
    mapper.insertIntent(
        intent.workflowType().name(),
        intent.requestId(),
        intent.bookingId(),
        intent.slotId(),
        intent.quantity(),
        intent.allocationOperationId(),
        intent.releaseOperationId(),
        intent.nextCheckAt());
    ReconciliationIntentEntity stored =
        mapper.selectByWorkflowRequest(intent.workflowType().name(), intent.requestId());
    if (stored == null) {
      throw new IllegalStateException("Reconciliation intent insert failed");
    }
    return new ReconciliationIntentId(stored.getId());
  }

  @Override
  public boolean resolveOpen(
      ReconciliationWorkflowType workflowType,
      String requestId,
      Long bookingId,
      ReconciliationOutcomeCode outcomeCode,
      LocalDateTime resolvedAt) {
    return mapper.resolveOpen(
            workflowType.name(), requestId, bookingId, outcomeCode.name(), resolvedAt)
        == 1;
  }

  @Override
  public List<ReconciliationIntent> previewDue(LocalDateTime now, int limit) {
    return mapper.selectDue(now, limit).stream().map(ReconciliationIntentEntity::toDomain).toList();
  }

  @Override
  @Transactional
  public ClaimedIntents claimDue(
      LocalDateTime now, int limit, String leaseOwner, LocalDateTime leaseExpiresAt) {
    List<ReconciliationIntent> claimed = new ArrayList<>();
    int leaseReclaimed = 0;
    for (ReconciliationIntentEntity candidate : mapper.selectDue(now, limit)) {
      if (mapper.claim(candidate.getId(), candidate.getVersion(), leaseOwner, now, leaseExpiresAt)
          == 1) {
        claimed.add(mapper.selectIntent(candidate.getId()).toDomain());
        if (candidate.getState() == ReconciliationIntentState.LEASED) {
          leaseReclaimed++;
        }
      }
    }
    return new ClaimedIntents(claimed, leaseReclaimed);
  }

  @Override
  public boolean resolve(
      long intentId,
      long expectedVersion,
      String leaseOwner,
      ReconciliationOutcomeCode outcomeCode,
      LocalDateTime resolvedAt) {
    return mapper.resolveLeased(
            intentId, expectedVersion, leaseOwner, outcomeCode.name(), resolvedAt)
        == 1;
  }

  @Override
  public boolean scheduleRetry(
      long intentId,
      long expectedVersion,
      String leaseOwner,
      String errorCode,
      LocalDateTime nextCheckAt,
      LocalDateTime updatedAt) {
    return mapper.retry(intentId, expectedVersion, leaseOwner, errorCode, nextCheckAt, updatedAt)
        == 1;
  }

  @Override
  public boolean exhaust(
      long intentId,
      long expectedVersion,
      String leaseOwner,
      String errorCode,
      LocalDateTime resolvedAt) {
    return mapper.exhaust(intentId, expectedVersion, leaseOwner, errorCode, resolvedAt) == 1;
  }

  @Override
  public long dueCount(LocalDateTime now) {
    return mapper.countDue(now);
  }

  @Override
  public long oldestDueAgeSeconds(LocalDateTime now) {
    return mapper.oldestDueAgeSeconds(now);
  }
}
