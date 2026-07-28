package com.yanerdan.venueflow.booking.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingApplicationDetails;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import com.yanerdan.venueflow.booking.domain.IdempotencyStatus;
import com.yanerdan.venueflow.booking.expiration.domain.TimeoutReservation;
import com.yanerdan.venueflow.booking.outbox.domain.OutboxEventFactory;
import com.yanerdan.venueflow.booking.outbox.persistence.OutboxEventEntity;
import com.yanerdan.venueflow.booking.outbox.persistence.OutboxEventMapper;
import com.yanerdan.venueflow.booking.reconciliation.application.port.ReconciliationIntentRepository;
import com.yanerdan.venueflow.booking.reconciliation.domain.NewReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationOutcomeCode;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationWorkflowType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("persistence")
public class BookingRepository {
  private static final String CREATE = "CREATE";
  private final BookingReservationMapper reservationMapper;
  private final BookingIdempotencyMapper idempotencyMapper;
  private final OutboxEventMapper outboxMapper;
  private final OutboxEventFactory outboxFactory;
  private final BookingStatusLogMapper statusLogMapper;
  private final ReconciliationIntentRepository reconciliationIntents;

  public BookingRepository(
      BookingReservationMapper reservationMapper,
      BookingIdempotencyMapper idempotencyMapper,
      OutboxEventMapper outboxMapper,
      OutboxEventFactory outboxFactory,
      BookingStatusLogMapper statusLogMapper,
      ReconciliationIntentRepository reconciliationIntents) {
    this.reservationMapper = reservationMapper;
    this.idempotencyMapper = idempotencyMapper;
    this.outboxMapper = outboxMapper;
    this.outboxFactory = outboxFactory;
    this.statusLogMapper = statusLogMapper;
    this.reconciliationIntents = reconciliationIntents;
  }

  @Transactional
  public ClaimResult claim(long userId, String key, String hash, long slotId, int quantity) {
    BookingIdempotencyEntity claim = new BookingIdempotencyEntity();
    LocalDateTime now = LocalDateTime.now();
    claim.setUserId(userId);
    claim.setOperation(CREATE);
    claim.setIdempotencyKey(key);
    claim.setRequestHash(hash);
    claim.setRequestId(UUID.randomUUID().toString());
    claim.setStatus(IdempotencyStatus.PROCESSING);
    claim.setVersion(0L);
    claim.setCreatedAt(now);
    claim.setUpdatedAt(now);
    if (idempotencyMapper.tryClaim(claim) == 1) {
      reconciliationIntents.create(
          new NewReconciliationIntent(
              ReconciliationWorkflowType.ALLOCATE,
              claim.getRequestId(),
              null,
              slotId,
              quantity,
              "allocate:" + claim.getRequestId(),
              "release:" + claim.getRequestId(),
              now.plusSeconds(30)));
      return new ClaimResult(ClaimResult.Kind.OWNER, claim.getRequestId(), null, null);
    }
    return classify(findClaim(userId, key), hash);
  }

  @Transactional
  public BookingReservation complete(
      String requestId,
      long userId,
      long slotId,
      int quantity,
      String allocationId,
      String releaseId,
      LocalDateTime expireAt) {
    return complete(
        requestId,
        userId,
        slotId,
        quantity,
        allocationId,
        releaseId,
        expireAt,
        BookingApplicationDetails.historical());
  }

  @Transactional
  public BookingReservation complete(
      String requestId,
      long userId,
      long slotId,
      int quantity,
      String allocationId,
      String releaseId,
      LocalDateTime expireAt,
      BookingApplicationDetails details) {
    return complete(
        requestId, userId, slotId, quantity, allocationId, releaseId, expireAt, details,
        null, null, null);
  }

  @Transactional
  public BookingReservation complete(
      String requestId,
      long userId,
      long slotId,
      int quantity,
      String allocationId,
      String releaseId,
      LocalDateTime expireAt,
      BookingApplicationDetails details,
      Long resourceId,
      String ownerDepartment,
      String assignedApproverExternalUserId) {
    LocalDateTime now = LocalDateTime.now();
    BookingReservationEntity entity = new BookingReservationEntity();
    entity.setBookingNo(UUID.randomUUID().toString());
    entity.setRequestId(requestId);
    entity.setUserId(userId);
    entity.setSlotId(slotId);
    entity.setResourceId(resourceId);
    entity.setOwnerDepartment(ownerDepartment);
    entity.setAssignedApproverExternalUserId(assignedApproverExternalUserId);
    entity.setQuantity(quantity);
    entity.setActivityTitle(details.activityTitle());
    entity.setApplicationPurpose(details.purpose());
    entity.setContactName(details.contactName());
    entity.setContactPhone(details.contactPhone());
    entity.setApplicationNote(details.note());
    entity.setStatus(BookingStatus.PENDING_CONFIRMATION);
    entity.setAllocationOperationId(allocationId);
    entity.setReleaseOperationId(releaseId);
    entity.setVersion(0L);
    entity.setCreatedAt(now);
    entity.setExpireAt(expireAt);
    entity.setTimeoutState("IDLE");
    entity.setTimeoutAttemptCount(0);
    entity.setTimeoutNextCheckAt(expireAt);
    entity.setUpdatedAt(now);
    reservationMapper.insert(entity);
    BookingReservation reservation = entity.toDomain();
    statusLogMapper.insertLog(
        entity.getId(), null, BookingStatus.PENDING_CONFIRMATION.name(), CREATE, null, now);
    int updated =
        idempotencyMapper.update(
            null,
            new LambdaUpdateWrapper<BookingIdempotencyEntity>()
                .eq(BookingIdempotencyEntity::getRequestId, requestId)
                .eq(BookingIdempotencyEntity::getStatus, IdempotencyStatus.PROCESSING)
                .set(BookingIdempotencyEntity::getStatus, IdempotencyStatus.SUCCEEDED)
                .set(BookingIdempotencyEntity::getBookingId, entity.getId())
                .set(BookingIdempotencyEntity::getUpdatedAt, now)
                .setSql("version = version + 1"));
    if (updated != 1) throw new IllegalStateException("Idempotency finalization failed");
    if (!reconciliationIntents.resolveOpen(
        ReconciliationWorkflowType.ALLOCATE,
        requestId,
        entity.getId(),
        ReconciliationOutcomeCode.ALREADY_CONSISTENT,
        now)) {
      throw new IllegalStateException("Allocation recovery intent finalization failed");
    }
    return reservation;
  }

  @Transactional
  public void fail(String requestId, String code, ReconciliationOutcomeCode reconciliationOutcome) {
    idempotencyMapper.update(
        null,
        new LambdaUpdateWrapper<BookingIdempotencyEntity>()
            .eq(BookingIdempotencyEntity::getRequestId, requestId)
            .eq(BookingIdempotencyEntity::getStatus, IdempotencyStatus.PROCESSING)
            .set(BookingIdempotencyEntity::getStatus, IdempotencyStatus.FAILED)
            .set(BookingIdempotencyEntity::getFailureCode, code)
            .set(BookingIdempotencyEntity::getUpdatedAt, LocalDateTime.now())
            .setSql("version = version + 1"));
    if (reconciliationOutcome != null) {
      reconciliationIntents.resolveOpen(
          ReconciliationWorkflowType.ALLOCATE,
          requestId,
          null,
          reconciliationOutcome,
          LocalDateTime.now());
    }
  }

  @Transactional(readOnly = true)
  public BookingReservation find(String bookingNo) {
    BookingReservationEntity entity =
        reservationMapper.selectOne(
            new LambdaQueryWrapper<BookingReservationEntity>()
                .eq(BookingReservationEntity::getBookingNo, bookingNo));
    return entity == null ? null : entity.toDomain();
  }

  @Transactional(readOnly = true)
  public BookingHistoryPage history(long userId, int pageNumber, int pageSize) {
    long offset = Math.multiplyExact((long) pageNumber, pageSize);
    List<BookingReservation> items =
        reservationMapper.selectHistory(userId, offset, pageSize).stream()
            .map(BookingReservationEntity::toDomain)
            .toList();
    return new BookingHistoryPage(
        items, reservationMapper.countHistory(userId), pageNumber, pageSize);
  }

  @Transactional(readOnly = true)
  public BookingHistoryPage managementHistory(BookingStatus status, int pageNumber, int pageSize) {
    long offset = Math.multiplyExact((long) pageNumber, pageSize);
    String statusValue = status == null ? null : status.name();
    List<BookingReservation> items =
        reservationMapper.selectManagementHistory(statusValue, offset, pageSize).stream()
            .map(BookingReservationEntity::toDomain)
            .toList();
    return new BookingHistoryPage(
        items, reservationMapper.countManagementHistory(statusValue), pageNumber, pageSize);
  }

  @Transactional(readOnly = true)
  public BookingHistoryPage managementHistory(
      BookingStatus status, String approverExternalUserId, int pageNumber, int pageSize) {
    long offset = Math.multiplyExact((long) pageNumber, pageSize);
    String statusValue = status == null ? null : status.name();
    List<BookingReservation> items =
        reservationMapper
            .selectAssignedManagementHistory(
                statusValue, approverExternalUserId, offset, pageSize)
            .stream()
            .map(BookingReservationEntity::toDomain)
            .toList();
    return new BookingHistoryPage(
        items,
        reservationMapper.countAssignedManagementHistory(statusValue, approverExternalUserId),
        pageNumber,
        pageSize);
  }

  @Transactional
  public BookingReservation confirm(String bookingNo) {
    return confirm(bookingNo, null, null);
  }

  @Transactional
  public BookingReservation confirm(String bookingNo, String reviewNote, String reviewerRole) {
    BookingReservationEntity current = findEntity(bookingNo);
    if (current == null) return null;
    if (current.getStatus() == BookingStatus.CONFIRMED) return current.toDomain();
    LocalDateTime now = LocalDateTime.now();
    if (current.getStatus() != BookingStatus.PENDING_CONFIRMATION) {
      throw new IllegalStateException("Reservation is terminal");
    }
    if (current.getExpireAt() == null || !now.isBefore(current.getExpireAt())) {
      throw new IllegalArgumentException("Reservation confirmation deadline expired");
    }
    int updated =
        reservationMapper.update(
            null,
            new LambdaUpdateWrapper<BookingReservationEntity>()
                .eq(BookingReservationEntity::getId, current.getId())
                .eq(BookingReservationEntity::getStatus, BookingStatus.PENDING_CONFIRMATION)
                .eq(BookingReservationEntity::getVersion, current.getVersion())
                .gt(BookingReservationEntity::getExpireAt, now)
                .and(
                    wrapper ->
                        wrapper
                            .isNull(BookingReservationEntity::getTimeoutLeaseOwner)
                            .or()
                            .le(BookingReservationEntity::getTimeoutLeaseExpiresAt, now))
                .set(BookingReservationEntity::getStatus, BookingStatus.CONFIRMED)
                .set(BookingReservationEntity::getConfirmedAt, now)
                .set(BookingReservationEntity::getReviewDecision, "APPROVED")
                .set(BookingReservationEntity::getReviewNote, reviewNote)
                .set(BookingReservationEntity::getReviewerRole, reviewerRole)
                .set(BookingReservationEntity::getReviewedAt, now)
                .set(BookingReservationEntity::getTimeoutState, "COMPLETED")
                .set(BookingReservationEntity::getTimeoutNextCheckAt, null)
                .set(BookingReservationEntity::getUpdatedAt, now)
                .setSql("version = version + 1"));
    if (updated != 1) return findEntity(bookingNo).toDomain();
    BookingReservationEntity confirmed = findEntity(bookingNo);
    statusLogMapper.insertLog(
        confirmed.getId(),
        BookingStatus.PENDING_CONFIRMATION.name(),
        BookingStatus.CONFIRMED.name(),
        "CONFIRM",
        null,
        now);
    outboxMapper.insert(OutboxEventEntity.from(outboxFactory.create(confirmed.toDomain())));
    return confirmed.toDomain();
  }

  @Transactional
  public boolean completeCheckIn(
      String bookingNo, long expectedVersion, LocalDateTime completedAt) {
    int updated =
        reservationMapper.update(
            null,
            new LambdaUpdateWrapper<BookingReservationEntity>()
                .eq(BookingReservationEntity::getBookingNo, bookingNo)
                .eq(BookingReservationEntity::getStatus, BookingStatus.CONFIRMED)
                .eq(BookingReservationEntity::getVersion, expectedVersion)
                .set(BookingReservationEntity::getStatus, BookingStatus.COMPLETED)
                .set(BookingReservationEntity::getCompletedAt, completedAt)
                .set(BookingReservationEntity::getUpdatedAt, completedAt)
                .setSql("version = version + 1"));
    if (updated != 1) return false;
    BookingReservationEntity completed = findEntity(bookingNo);
    statusLogMapper.insertLog(
        completed.getId(),
        BookingStatus.CONFIRMED.name(),
        BookingStatus.COMPLETED.name(),
        "CHECK_IN",
        null,
        completedAt);
    outboxMapper.insert(OutboxEventEntity.from(outboxFactory.create(completed.toDomain())));
    return true;
  }

  @Transactional(readOnly = true)
  public boolean hasLiveTimeoutLease(String bookingNo) {
    BookingReservationEntity entity = findEntity(bookingNo);
    return entity != null
        && entity.getTimeoutLeaseOwner() != null
        && entity.getTimeoutLeaseExpiresAt() != null
        && entity.getTimeoutLeaseExpiresAt().isAfter(LocalDateTime.now());
  }

  @Transactional
  public void prepareCancellation(BookingReservation reservation) {
    reconciliationIntents.create(
        new NewReconciliationIntent(
            ReconciliationWorkflowType.RELEASE,
            reservation.requestId(),
            reservation.id(),
            reservation.slotId(),
            reservation.quantity(),
            reservation.allocationOperationId(),
            reservation.releaseOperationId(),
            LocalDateTime.now().plusSeconds(30)));
  }

  @Transactional
  public boolean cancelAndResolve(String bookingNo, long expectedVersion) {
    return cancelAndResolve(
        bookingNo,
        expectedVersion,
        "USER_CANCELLED",
        "CANCELLED",
        null,
        "APPLICANT");
  }

  @Transactional
  public boolean cancelAndResolve(
      String bookingNo,
      long expectedVersion,
      String terminalReason,
      String reviewDecision,
      String reviewNote,
      String reviewerRole) {
    LocalDateTime now = LocalDateTime.now();
    BookingReservationEntity before = findEntity(bookingNo);
    boolean updated =
        reservationMapper.update(
                null,
                new LambdaUpdateWrapper<BookingReservationEntity>()
                    .eq(BookingReservationEntity::getBookingNo, bookingNo)
                    .in(
                        BookingReservationEntity::getStatus,
                        BookingStatus.PENDING_CONFIRMATION,
                        BookingStatus.CONFIRMED)
                    .eq(BookingReservationEntity::getVersion, expectedVersion)
                    .and(
                        wrapper ->
                            wrapper
                                .isNull(BookingReservationEntity::getTimeoutLeaseOwner)
                                .or()
                                .le(BookingReservationEntity::getTimeoutLeaseExpiresAt, now))
                    .set(BookingReservationEntity::getStatus, BookingStatus.CANCELLED)
                    .set(BookingReservationEntity::getCancelledAt, now)
                    .set(BookingReservationEntity::getTerminalReason, terminalReason)
                    .set(BookingReservationEntity::getReviewDecision, reviewDecision)
                    .set(BookingReservationEntity::getReviewNote, reviewNote)
                    .set(BookingReservationEntity::getReviewerRole, reviewerRole)
                    .set(BookingReservationEntity::getReviewedAt, now)
                    .set(BookingReservationEntity::getTimeoutState, "COMPLETED")
                    .set(BookingReservationEntity::getTimeoutNextCheckAt, null)
                    .set(BookingReservationEntity::getUpdatedAt, now)
                    .setSql("version = version + 1"))
            == 1;
    if (updated) {
      BookingReservationEntity entity =
          reservationMapper.selectOne(
              new LambdaQueryWrapper<BookingReservationEntity>()
                  .eq(BookingReservationEntity::getBookingNo, bookingNo));
      statusLogMapper.insertLog(
          entity.getId(),
          before.getStatus().name(),
          BookingStatus.CANCELLED.name(),
          "CANCEL",
          terminalReason,
          now);
      outboxMapper.insert(OutboxEventEntity.from(outboxFactory.create(entity.toDomain())));
      if (!reconciliationIntents.resolveOpen(
          ReconciliationWorkflowType.RELEASE,
          entity.getRequestId(),
          entity.getId(),
          ReconciliationOutcomeCode.CANCELLATION_COMPLETED,
          now)) {
        throw new IllegalStateException("Cancellation recovery intent finalization failed");
      }
    } else {
      BookingReservationEntity current =
          reservationMapper.selectOne(
              new LambdaQueryWrapper<BookingReservationEntity>()
                  .eq(BookingReservationEntity::getBookingNo, bookingNo));
      if (current != null && current.getStatus() == BookingStatus.CANCELLED) {
        reconciliationIntents.resolveOpen(
            ReconciliationWorkflowType.RELEASE,
            current.getRequestId(),
            current.getId(),
            ReconciliationOutcomeCode.ALREADY_CANCELLED,
            now);
      }
    }
    return updated;
  }

  @Transactional(readOnly = true)
  public BookingReservation findById(long bookingId) {
    BookingReservationEntity entity = reservationMapper.selectById(bookingId);
    return entity == null ? null : entity.toDomain();
  }

  @Transactional(readOnly = true)
  public BookingReservation findByRequestId(String requestId) {
    BookingReservationEntity entity =
        reservationMapper.selectOne(
            new LambdaQueryWrapper<BookingReservationEntity>()
                .eq(BookingReservationEntity::getRequestId, requestId));
    return entity == null ? null : entity.toDomain();
  }

  @Transactional
  public ReconciliationOutcomeCode completeReconciledCancellation(
      ReconciliationIntent intent, String leaseOwner) {
    BookingReservationEntity entity = reservationMapper.selectById(intent.bookingId());
    if (entity == null) {
      throw new IllegalStateException("Cancellation booking does not exist");
    }
    LocalDateTime now = LocalDateTime.now();
    ReconciliationOutcomeCode outcome;
    if (entity.getStatus() == BookingStatus.CANCELLED) {
      outcome = ReconciliationOutcomeCode.ALREADY_CANCELLED;
    } else {
      if (entity.getStatus() != BookingStatus.CONFIRMED
          && entity.getStatus() != BookingStatus.PENDING_CONFIRMATION) {
        throw new IllegalStateException("Cancellation booking state conflicts");
      }
      BookingStatus previousStatus = entity.getStatus();
      int updated =
          reservationMapper.update(
              null,
              new LambdaUpdateWrapper<BookingReservationEntity>()
                  .eq(BookingReservationEntity::getId, entity.getId())
                  .eq(BookingReservationEntity::getStatus, previousStatus)
                  .eq(BookingReservationEntity::getVersion, entity.getVersion())
                  .and(
                      wrapper ->
                          wrapper
                              .isNull(BookingReservationEntity::getTimeoutLeaseOwner)
                              .or()
                              .le(BookingReservationEntity::getTimeoutLeaseExpiresAt, now))
                  .set(BookingReservationEntity::getStatus, BookingStatus.CANCELLED)
                  .set(BookingReservationEntity::getCancelledAt, now)
                  .set(BookingReservationEntity::getTerminalReason, "USER_CANCELLED")
                  .set(BookingReservationEntity::getTimeoutState, "COMPLETED")
                  .set(BookingReservationEntity::getTimeoutNextCheckAt, null)
                  .set(BookingReservationEntity::getUpdatedAt, now)
                  .setSql("version = version + 1"));
      if (updated != 1) {
        throw new IllegalStateException("Cancellation booking changed concurrently");
      }
      entity =
          reservationMapper.selectOne(
              new LambdaQueryWrapper<BookingReservationEntity>()
                  .eq(BookingReservationEntity::getId, entity.getId()));
      statusLogMapper.insertLog(
          entity.getId(),
          previousStatus.name(),
          BookingStatus.CANCELLED.name(),
          "RECONCILIATION",
          "USER_CANCELLED",
          now);
      outboxMapper.insert(OutboxEventEntity.from(outboxFactory.create(entity.toDomain())));
      outcome = ReconciliationOutcomeCode.CANCELLATION_COMPLETED;
    }
    if (!reconciliationIntents.resolve(intent.id(), intent.version(), leaseOwner, outcome, now)) {
      throw new IllegalStateException("Cancellation reconciliation lease was lost");
    }
    return outcome;
  }

  @Transactional
  public boolean completeExpiration(TimeoutReservation timeout) {
    LocalDateTime now = LocalDateTime.now();
    int updated =
        reservationMapper.update(
            null,
            new LambdaUpdateWrapper<BookingReservationEntity>()
                .eq(BookingReservationEntity::getId, timeout.id())
                .eq(BookingReservationEntity::getVersion, timeout.version())
                .eq(BookingReservationEntity::getStatus, BookingStatus.PENDING_CONFIRMATION)
                .eq(BookingReservationEntity::getTimeoutState, "LEASED")
                .eq(BookingReservationEntity::getTimeoutLeaseOwner, timeout.leaseOwner())
                .gt(BookingReservationEntity::getTimeoutLeaseExpiresAt, now)
                .set(BookingReservationEntity::getStatus, BookingStatus.EXPIRED)
                .set(BookingReservationEntity::getExpiredAt, now)
                .set(BookingReservationEntity::getTerminalReason, "CONFIRMATION_TIMEOUT")
                .set(BookingReservationEntity::getTimeoutState, "COMPLETED")
                .set(BookingReservationEntity::getTimeoutLeaseOwner, null)
                .set(BookingReservationEntity::getTimeoutLeaseExpiresAt, null)
                .set(BookingReservationEntity::getTimeoutNextCheckAt, null)
                .set(BookingReservationEntity::getTimeoutLastErrorCode, null)
                .set(BookingReservationEntity::getUpdatedAt, now)
                .setSql("version = version + 1"));
    if (updated != 1) return false;
    BookingReservationEntity expired = reservationMapper.selectById(timeout.id());
    statusLogMapper.insertLog(
        expired.getId(),
        BookingStatus.PENDING_CONFIRMATION.name(),
        BookingStatus.EXPIRED.name(),
        "TIMEOUT",
        "CONFIRMATION_TIMEOUT",
        now);
    outboxMapper.insert(OutboxEventEntity.from(outboxFactory.create(expired.toDomain())));
    return true;
  }

  private BookingIdempotencyEntity findClaim(long userId, String key) {
    return idempotencyMapper.selectOne(
        new LambdaQueryWrapper<BookingIdempotencyEntity>()
            .eq(BookingIdempotencyEntity::getUserId, userId)
            .eq(BookingIdempotencyEntity::getOperation, CREATE)
            .eq(BookingIdempotencyEntity::getIdempotencyKey, key));
  }

  private BookingReservationEntity findEntity(String bookingNo) {
    return reservationMapper.selectOne(
        new LambdaQueryWrapper<BookingReservationEntity>()
            .eq(BookingReservationEntity::getBookingNo, bookingNo));
  }

  private ClaimResult classify(BookingIdempotencyEntity claim, String hash) {
    if (claim == null || !claim.getRequestHash().equals(hash)) {
      return new ClaimResult(ClaimResult.Kind.CONFLICT, null, null, null);
    }
    if (claim.getStatus() == IdempotencyStatus.SUCCEEDED) {
      BookingReservationEntity entity = reservationMapper.selectById(claim.getBookingId());
      return new ClaimResult(
          ClaimResult.Kind.SUCCEEDED, claim.getRequestId(), entity.toDomain(), null);
    }
    return new ClaimResult(
        claim.getStatus() == IdempotencyStatus.PROCESSING
            ? ClaimResult.Kind.PROCESSING
            : ClaimResult.Kind.FAILED,
        claim.getRequestId(),
        null,
        claim.getFailureCode());
  }

  public record BookingHistoryPage(
      List<BookingReservation> items, long totalElements, int pageNumber, int pageSize) {
    public BookingHistoryPage {
      items = List.copyOf(items);
    }
  }
}
