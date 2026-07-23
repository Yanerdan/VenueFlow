package com.yanerdan.venueflow.booking.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import com.yanerdan.venueflow.booking.domain.IdempotencyStatus;
import com.yanerdan.venueflow.booking.outbox.domain.OutboxEventFactory;
import com.yanerdan.venueflow.booking.outbox.persistence.OutboxEventEntity;
import com.yanerdan.venueflow.booking.outbox.persistence.OutboxEventMapper;
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

  public BookingRepository(
      BookingReservationMapper reservationMapper,
      BookingIdempotencyMapper idempotencyMapper,
      OutboxEventMapper outboxMapper,
      OutboxEventFactory outboxFactory) {
    this.reservationMapper = reservationMapper;
    this.idempotencyMapper = idempotencyMapper;
    this.outboxMapper = outboxMapper;
    this.outboxFactory = outboxFactory;
  }

  @Transactional
  public ClaimResult claim(long userId, String key, String hash) {
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
    return reservation;
  }

  @Transactional
  public void fail(String requestId, String code) {
    idempotencyMapper.update(
        null,
        new LambdaUpdateWrapper<BookingIdempotencyEntity>()
            .eq(BookingIdempotencyEntity::getRequestId, requestId)
            .eq(BookingIdempotencyEntity::getStatus, IdempotencyStatus.PROCESSING)
            .set(BookingIdempotencyEntity::getStatus, IdempotencyStatus.FAILED)
            .set(BookingIdempotencyEntity::getFailureCode, code)
            .set(BookingIdempotencyEntity::getUpdatedAt, LocalDateTime.now())
            .setSql("version = version + 1"));
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
  public boolean cancel(String bookingNo, long expectedVersion) {
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
    }
    return updated;
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
