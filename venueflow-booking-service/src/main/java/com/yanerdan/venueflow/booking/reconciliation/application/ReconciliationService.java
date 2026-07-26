package com.yanerdan.venueflow.booking.reconciliation.application;

import com.yanerdan.venueflow.booking.application.BookingException;
import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient;
import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient.ResourceOperation;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import com.yanerdan.venueflow.booking.persistence.BookingRepository;
import com.yanerdan.venueflow.booking.reconciliation.application.port.ReconciliationIntentRepository;
import com.yanerdan.venueflow.booking.reconciliation.application.port.ReconciliationIntentRepository.ClaimedIntents;
import com.yanerdan.venueflow.booking.reconciliation.config.ReconciliationProperties;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIssueCode;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationOutcomeCode;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationWorkflowType;
import com.yanerdan.venueflow.booking.reconciliation.persistence.ReconciliationAuditRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("persistence & reconciliation")
public class ReconciliationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliationService.class);
  private final ReconciliationIntentRepository intents;
  private final ReconciliationAuditRepository audit;
  private final BookingRepository bookings;
  private final ResourceCapacityClient resource;
  private final ReconciliationProperties properties;
  private final MeterRegistry meters;
  private final AtomicLong dueDepth = new AtomicLong();
  private final AtomicLong oldestAge = new AtomicLong();

  public ReconciliationService(
      ReconciliationIntentRepository intents,
      ReconciliationAuditRepository audit,
      BookingRepository bookings,
      ResourceCapacityClient resource,
      ReconciliationProperties properties,
      MeterRegistry meters) {
    this.intents = intents;
    this.audit = audit;
    this.bookings = bookings;
    this.resource = resource;
    this.properties = properties;
    this.meters = meters;
    meters.gauge("venueflow.booking.reconciliation.due", dueDepth);
    meters.gauge("venueflow.booking.reconciliation.oldest.seconds", oldestAge);
  }

  public List<ReconciliationIntent> preview() {
    LocalDateTime now = LocalDateTime.now();
    updateBacklog(now);
    return intents.previewDue(now, properties.batchSize());
  }

  public ReconciliationSummary runOnce(String trigger, String reason) {
    LocalDateTime now = LocalDateTime.now();
    String owner = UUID.randomUUID().toString();
    long runId =
        audit.startRun(
            UUID.randomUUID().toString(),
            trigger,
            owner,
            now.plus(properties.leaseDuration()),
            reason);
    ClaimedIntents claim =
        intents.claimDue(now, properties.batchSize(), owner, now.plus(properties.leaseDuration()));
    List<ReconciliationIntent> claimed = claim.intents();
    MutableSummary summary = new MutableSummary(claimed.size(), claim.leaseReclaimed());
    meters.counter("venueflow.booking.reconciliation.claimed").increment(claimed.size());
    meters
        .counter("venueflow.booking.reconciliation.lease.reclaimed")
        .increment(claim.leaseReclaimed());
    try {
      for (ReconciliationIntent intent : claimed) {
        process(runId, owner, intent, summary);
      }
      ReconciliationSummary result = summary.freeze();
      audit.completeRun(runId, result, "COMPLETED");
      updateBacklog(LocalDateTime.now());
      LOGGER.info(
          "booking_reconciliation outcome=COMPLETED claimed={} consistent={} repaired={} unresolved={} failed={} leaseReclaimed={}",
          result.claimed(),
          result.consistent(),
          result.repaired(),
          result.unresolved(),
          result.failed(),
          result.leaseReclaimed());
      return result;
    } catch (RuntimeException exception) {
      ReconciliationSummary result = summary.freeze();
      audit.completeRun(runId, result, "FAILED");
      throw exception;
    }
  }

  private void process(
      long runId, String owner, ReconciliationIntent intent, MutableSummary summary) {
    try {
      if (intent.workflowType() == ReconciliationWorkflowType.ALLOCATE) {
        reconcileAllocation(runId, owner, intent, summary);
      } else {
        reconcileCancellation(runId, owner, intent, summary);
      }
    } catch (RuntimeException exception) {
      summary.failed++;
      retry(intent, owner, ReconciliationIssueCode.PERSISTENCE_FAILURE, summary);
      LOGGER.warn(
          "booking_reconciliation outcome=FAILED intentId={} workflow={} attempt={} code={}",
          intent.id(),
          intent.workflowType(),
          intent.attemptCount(),
          ReconciliationIssueCode.PERSISTENCE_FAILURE);
    }
  }

  private void reconcileAllocation(
      long runId, String owner, ReconciliationIntent intent, MutableSummary summary) {
    BookingReservation booking = bookings.findByRequestId(intent.requestId());
    if (booking != null
        && (booking.status() == BookingStatus.PENDING_CONFIRMATION
            || booking.status() == BookingStatus.CONFIRMED)) {
      resolve(intent, owner, ReconciliationOutcomeCode.ALREADY_CONSISTENT, summary, false);
      return;
    }
    OperationLookup allocationLookup = lookup(intent, intent.allocationOperationId(), summary);
    if (!allocationLookup.available()) {
      return;
    }
    Optional<ResourceOperation> allocation = allocationLookup.operation();
    if (allocation.isEmpty()) {
      resolve(intent, owner, ReconciliationOutcomeCode.NO_ALLOCATION, summary, false);
      return;
    }
    if (!matches(allocation.orElseThrow(), "ALLOCATE", intent)) {
      retry(intent, owner, ReconciliationIssueCode.OPERATION_MISMATCH, summary);
      return;
    }
    if (releaseAndProve(runId, intent)) {
      resolve(intent, owner, ReconciliationOutcomeCode.ORPHAN_RELEASED, summary, true);
    } else {
      retry(intent, owner, ReconciliationIssueCode.OPERATION_UNKNOWN, summary);
    }
  }

  private void reconcileCancellation(
      long runId, String owner, ReconciliationIntent intent, MutableSummary summary) {
    BookingReservation booking =
        intent.bookingId() == null ? null : bookings.findById(intent.bookingId());
    if (booking == null
        || (booking.status() != BookingStatus.PENDING_CONFIRMATION
            && booking.status() != BookingStatus.CONFIRMED
            && booking.status() != BookingStatus.CANCELLED)) {
      retry(intent, owner, ReconciliationIssueCode.BOOKING_STATE_CONFLICT, summary);
      return;
    }
    if (booking.status() == BookingStatus.CANCELLED) {
      resolve(intent, owner, ReconciliationOutcomeCode.ALREADY_CANCELLED, summary, false);
      return;
    }
    OperationLookup releaseLookup = lookup(intent, intent.releaseOperationId(), summary);
    if (!releaseLookup.available()) {
      return;
    }
    Optional<ResourceOperation> release = releaseLookup.operation();
    if (release.isPresent() && !matches(release.orElseThrow(), "RELEASE", intent)) {
      retry(intent, owner, ReconciliationIssueCode.OPERATION_MISMATCH, summary);
      return;
    }
    boolean proven = release.isPresent() || releaseAndProve(runId, intent);
    if (!proven) {
      retry(intent, owner, ReconciliationIssueCode.OPERATION_UNKNOWN, summary);
      return;
    }
    ReconciliationOutcomeCode outcome = bookings.completeReconciledCancellation(intent, owner);
    audit.resolveIssues(intent.id());
    summary.repaired++;
    meters.counter("venueflow.booking.reconciliation.outcome", "code", outcome.name()).increment();
  }

  private OperationLookup lookup(
      ReconciliationIntent intent, String operationId, MutableSummary summary) {
    try {
      return new OperationLookup(true, resource.findOperation(intent.slotId(), operationId));
    } catch (BookingException exception) {
      retry(intent, intent.leaseOwner(), ReconciliationIssueCode.RESOURCE_UNAVAILABLE, summary);
      return new OperationLookup(false, Optional.empty());
    }
  }

  private boolean releaseAndProve(long runId, ReconciliationIntent intent) {
    long actionId =
        audit.startRepair(
            intent.id(),
            runId,
            intent.attemptCount(),
            intent.workflowType() == ReconciliationWorkflowType.ALLOCATE
                ? "RELEASE_ORPHAN"
                : "COMPLETE_CANCELLATION",
            intent.workflowType() == ReconciliationWorkflowType.ALLOCATE
                ? "PROVEN_ALLOCATION"
                : "PENDING_CANCELLATION",
            intent.releaseOperationId());
    try {
      resource.release(intent.slotId(), intent.releaseOperationId(), intent.quantity());
      audit.completeRepair(actionId, "SUCCEEDED", "RELEASE_CONFIRMED");
      meters.counter("venueflow.booking.reconciliation.action", "outcome", "SUCCEEDED").increment();
      return true;
    } catch (BookingException exception) {
      try {
        Optional<ResourceOperation> release =
            resource.findOperation(intent.slotId(), intent.releaseOperationId());
        if (release.isPresent() && matches(release.orElseThrow(), "RELEASE", intent)) {
          audit.completeRepair(actionId, "SUCCEEDED", "RELEASE_PROVEN_BY_LOOKUP");
          meters
              .counter("venueflow.booking.reconciliation.action", "outcome", "SUCCEEDED")
              .increment();
          return true;
        }
      } catch (BookingException ignored) {
        // The caller will retain the intent through a bounded retry transition.
      }
      audit.completeRepair(actionId, "UNKNOWN", "RELEASE_UNPROVEN");
      meters.counter("venueflow.booking.reconciliation.action", "outcome", "UNKNOWN").increment();
      return false;
    }
  }

  private static boolean matches(
      ResourceOperation operation, String type, ReconciliationIntent intent) {
    return operation
            .operationId()
            .equals(
                "ALLOCATE".equals(type)
                    ? intent.allocationOperationId()
                    : intent.releaseOperationId())
        && type.equals(operation.operationType())
        && operation.quantity() == intent.quantity();
  }

  private void resolve(
      ReconciliationIntent intent,
      String owner,
      ReconciliationOutcomeCode outcome,
      MutableSummary summary,
      boolean repaired) {
    if (!intents.resolve(intent.id(), intent.version(), owner, outcome, LocalDateTime.now())) {
      throw new IllegalStateException("Reconciliation lease was lost");
    }
    audit.resolveIssues(intent.id());
    if (repaired) {
      summary.repaired++;
    } else {
      summary.consistent++;
    }
    meters.counter("venueflow.booking.reconciliation.outcome", "code", outcome.name()).increment();
  }

  private void retry(
      ReconciliationIntent intent,
      String owner,
      ReconciliationIssueCode issue,
      MutableSummary summary) {
    audit.recordIssue(
        intent.id(),
        issue.name(),
        issue == ReconciliationIssueCode.OPERATION_MISMATCH ? "CRITICAL" : "ERROR");
    LocalDateTime now = LocalDateTime.now();
    boolean updated;
    if (intent.attemptCount() >= properties.maxAttempts()) {
      updated = intents.exhaust(intent.id(), intent.version(), owner, issue.name(), now);
    } else {
      updated =
          intents.scheduleRetry(
              intent.id(),
              intent.version(),
              owner,
              issue.name(),
              now.plus(backoff(intent.attemptCount())),
              now);
    }
    if (!updated) {
      throw new IllegalStateException("Reconciliation retry lease was lost");
    }
    summary.unresolved++;
    meters.counter("venueflow.booking.reconciliation.outcome", "code", issue.name()).increment();
  }

  private Duration backoff(int attempt) {
    long multiplier = 1L << Math.min(Math.max(0, attempt - 1), 20);
    Duration candidate = properties.initialBackoff().multipliedBy(multiplier);
    return candidate.compareTo(properties.maxBackoff()) > 0 ? properties.maxBackoff() : candidate;
  }

  private void updateBacklog(LocalDateTime now) {
    dueDepth.set(intents.dueCount(now));
    oldestAge.set(intents.oldestDueAgeSeconds(now));
  }

  private static final class MutableSummary {
    private final int claimed;
    private int consistent;
    private int repaired;
    private int unresolved;
    private int failed;
    private final int leaseReclaimed;

    private MutableSummary(int claimed, int leaseReclaimed) {
      this.claimed = claimed;
      this.leaseReclaimed = leaseReclaimed;
    }

    private ReconciliationSummary freeze() {
      return new ReconciliationSummary(
          claimed, consistent, repaired, unresolved, failed, leaseReclaimed);
    }
  }

  private record OperationLookup(boolean available, Optional<ResourceOperation> operation) {}
}
