package com.yanerdan.venueflow.booking.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import com.yanerdan.venueflow.booking.domain.IdempotencyStatus;
import com.yanerdan.venueflow.booking.outbox.domain.OutboxEventFactory;
import com.yanerdan.venueflow.booking.outbox.persistence.OutboxEventEntity;
import com.yanerdan.venueflow.booking.outbox.persistence.OutboxEventMapper;
import com.yanerdan.venueflow.booking.reconciliation.application.port.ReconciliationIntentRepository;
import com.yanerdan.venueflow.booking.reconciliation.domain.NewReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationOutcomeCode;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationWorkflowType;
import java.time.LocalDateTime;
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
  private final ReconciliationIntentRepository reconciliationIntents;

  public BookingRepository(
      BookingReservationMapper reservationMapper,
      BookingIdempotencyMapper idempotencyMapper,
      OutboxEventMapper outboxMapper,
      OutboxEventFactory outboxFactory,
      ReconciliationIntentRepository reconciliationIntents) {
    this.reservationMapper = reservationMapper;
    this.idempotencyMapper = idempotencyMapper;
    this.outboxMapper = outboxMapper;
    this.outboxFactory = outboxFactory;
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
      String releaseId) {
    LocalDateTime now = LocalDateTime.now();
    BookingReservationEntity entity = new BookingReservationEntity();
    entity.setBookingNo(UUID.randomUUID().toString());
    entity.setRequestId(requestId);
    entity.setUserId(userId);
    entity.setSlotId(slotId);
    entity.setQuantity(quantity);
    entity.setStatus(BookingStatus.CONFIRMED);
    entity.setAllocationOperationId(allocationId);
    entity.setReleaseOperationId(releaseId);
    entity.setVersion(0L);
    entity.setCreatedAt(now);
    entity.setConfirmedAt(now);
    entity.setUpdatedAt(now);
    reservationMapper.insert(entity);
    BookingReservation reservation = entity.toDomain();
    outboxMapper.insert(OutboxEventEntity.from(outboxFactory.create(reservation)));
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
    LocalDateTime now = LocalDateTime.now();
    boolean updated =
        reservationMapper.update(
                null,
                new LambdaUpdateWrapper<BookingReservationEntity>()
                    .eq(BookingReservationEntity::getBookingNo, bookingNo)
                    .eq(BookingReservationEntity::getStatus, BookingStatus.CONFIRMED)
                    .eq(BookingReservationEntity::getVersion, expectedVersion)
                    .set(BookingReservationEntity::getStatus, BookingStatus.CANCELLED)
                    .set(BookingReservationEntity::getCancelledAt, now)
                    .set(BookingReservationEntity::getUpdatedAt, now)
                    .setSql("version = version + 1"))
            == 1;
    if (updated) {
      BookingReservationEntity entity =
          reservationMapper.selectOne(
              new LambdaQueryWrapper<BookingReservationEntity>()
                  .eq(BookingReservationEntity::getBookingNo, bookingNo));
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
      if (entity.getStatus() != BookingStatus.CONFIRMED) {
        throw new IllegalStateException("Cancellation booking state conflicts");
      }
      int updated =
          reservationMapper.update(
              null,
              new LambdaUpdateWrapper<BookingReservationEntity>()
                  .eq(BookingReservationEntity::getId, entity.getId())
                  .eq(BookingReservationEntity::getStatus, BookingStatus.CONFIRMED)
                  .eq(BookingReservationEntity::getVersion, entity.getVersion())
                  .set(BookingReservationEntity::getStatus, BookingStatus.CANCELLED)
                  .set(BookingReservationEntity::getCancelledAt, now)
                  .set(BookingReservationEntity::getUpdatedAt, now)
                  .setSql("version = version + 1"));
      if (updated != 1) {
        throw new IllegalStateException("Cancellation booking changed concurrently");
      }
      entity =
          reservationMapper.selectOne(
              new LambdaQueryWrapper<BookingReservationEntity>()
                  .eq(BookingReservationEntity::getId, entity.getId()));
      outboxMapper.insert(OutboxEventEntity.from(outboxFactory.create(entity.toDomain())));
      outcome = ReconciliationOutcomeCode.CANCELLATION_COMPLETED;
    }
    if (!reconciliationIntents.resolve(intent.id(), intent.version(), leaseOwner, outcome, now)) {
      throw new IllegalStateException("Cancellation reconciliation lease was lost");
    }
    return outcome;
  }

  private BookingIdempotencyEntity findClaim(long userId, String key) {
    return idempotencyMapper.selectOne(
        new LambdaQueryWrapper<BookingIdempotencyEntity>()
            .eq(BookingIdempotencyEntity::getUserId, userId)
            .eq(BookingIdempotencyEntity::getOperation, CREATE)
            .eq(BookingIdempotencyEntity::getIdempotencyKey, key));
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
}
