package com.yanerdan.venueflow.booking.expiration.persistence;

import com.yanerdan.venueflow.booking.expiration.domain.TimeoutReservation;
import com.yanerdan.venueflow.booking.persistence.BookingReservationEntity;
import com.yanerdan.venueflow.booking.persistence.BookingReservationMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("persistence & expiration")
public class ExpirationRepository {
  private final BookingReservationMapper mapper;

  public ExpirationRepository(BookingReservationMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public List<TimeoutReservation> preview(LocalDateTime now, int limit) {
    return mapper.selectTimeoutDue(now, limit).stream()
        .map(ExpirationRepository::toTimeout)
        .toList();
  }

  @Transactional
  public ClaimedTimeouts claim(
      LocalDateTime now, int limit, String owner, LocalDateTime leaseExpiresAt) {
    List<BookingReservationEntity> due = mapper.selectTimeoutDue(now, limit);
    List<TimeoutReservation> claimed = new ArrayList<>();
    int reclaimed = 0;
    for (BookingReservationEntity candidate : due) {
      boolean wasLeased = "LEASED".equals(candidate.getTimeoutState());
      if (mapper.claimTimeout(candidate.getId(), candidate.getVersion(), owner, now, leaseExpiresAt)
          == 1) {
        claimed.add(toTimeout(mapper.selectById(candidate.getId())));
        if (wasLeased) reclaimed++;
      }
    }
    return new ClaimedTimeouts(List.copyOf(claimed), reclaimed);
  }

  @Transactional
  public boolean retry(
      TimeoutReservation timeout, String errorCode, LocalDateTime nextCheckAt, LocalDateTime now) {
    return mapper.retryTimeout(
            timeout.id(), timeout.version(), timeout.leaseOwner(), errorCode, nextCheckAt, now)
        == 1;
  }

  @Transactional
  public boolean exhaust(TimeoutReservation timeout, String errorCode, LocalDateTime now) {
    return mapper.exhaustTimeout(
            timeout.id(), timeout.version(), timeout.leaseOwner(), errorCode, now)
        == 1;
  }

  public long dueCount(LocalDateTime now) {
    return mapper.countTimeoutDue(now);
  }

  public long oldestDueAgeSeconds(LocalDateTime now) {
    return mapper.oldestTimeoutAgeSeconds(now);
  }

  private static TimeoutReservation toTimeout(BookingReservationEntity entity) {
    return new TimeoutReservation(
        entity.getId(),
        entity.getBookingNo(),
        entity.getSlotId(),
        entity.getQuantity(),
        entity.getReleaseOperationId(),
        entity.getVersion(),
        entity.getTimeoutAttemptCount(),
        entity.getTimeoutLeaseOwner());
  }

  public record ClaimedTimeouts(List<TimeoutReservation> reservations, int leaseReclaimed) {}
}
