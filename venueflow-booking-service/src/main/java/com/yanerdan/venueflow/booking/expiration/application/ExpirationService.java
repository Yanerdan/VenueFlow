package com.yanerdan.venueflow.booking.expiration.application;

import com.yanerdan.venueflow.booking.application.BookingException;
import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient;
import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient.ResourceOperation;
import com.yanerdan.venueflow.booking.expiration.config.ExpirationProperties;
import com.yanerdan.venueflow.booking.expiration.domain.TimeoutReservation;
import com.yanerdan.venueflow.booking.expiration.persistence.ExpirationRepository;
import com.yanerdan.venueflow.booking.expiration.persistence.ExpirationRepository.ClaimedTimeouts;
import com.yanerdan.venueflow.booking.persistence.BookingRepository;
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
@Profile("persistence & expiration")
public class ExpirationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExpirationService.class);
  private final ExpirationRepository expirations;
  private final BookingRepository bookings;
  private final ResourceCapacityClient resource;
  private final ExpirationProperties properties;
  private final MeterRegistry meters;
  private final AtomicLong dueDepth = new AtomicLong();
  private final AtomicLong oldestAge = new AtomicLong();

  public ExpirationService(
      ExpirationRepository expirations,
      BookingRepository bookings,
      ResourceCapacityClient resource,
      ExpirationProperties properties,
      MeterRegistry meters) {
    this.expirations = expirations;
    this.bookings = bookings;
    this.resource = resource;
    this.properties = properties;
    this.meters = meters;
    meters.gauge("venueflow.booking.expiration.due", dueDepth);
    meters.gauge("venueflow.booking.expiration.oldest.seconds", oldestAge);
  }

  public List<TimeoutReservation> preview() {
    updateBacklog();
    return expirations.preview(LocalDateTime.now(), properties.batchSize());
  }

  public ExpirationSummary runOnce(String trigger, String reason) {
    LocalDateTime now = LocalDateTime.now();
    String owner = UUID.randomUUID().toString();
    ClaimedTimeouts batch =
        expirations.claim(now, properties.batchSize(), owner, now.plus(properties.leaseDuration()));
    int expired = 0;
    int retried = 0;
    int lost = 0;
    for (TimeoutReservation timeout : batch.reservations()) {
      Outcome outcome = process(timeout);
      if (outcome == Outcome.EXPIRED) expired++;
      if (outcome == Outcome.RETRIED) retried++;
      if (outcome == Outcome.LOST) lost++;
    }
    meters.counter("venueflow.booking.expiration.claimed").increment(batch.reservations().size());
    meters.counter("venueflow.booking.expiration.expired").increment(expired);
    meters.counter("venueflow.booking.expiration.retry").increment(retried);
    meters
        .counter("venueflow.booking.expiration.lease.reclaimed")
        .increment(batch.leaseReclaimed());
    updateBacklog();
    LOGGER.info(
        "booking_expiration trigger={} outcome=COMPLETED claimed={} expired={} retried={} lost={} leaseReclaimed={} reasonPresent={}",
        trigger,
        batch.reservations().size(),
        expired,
        retried,
        lost,
        batch.leaseReclaimed(),
        reason != null);
    return new ExpirationSummary(
        batch.reservations().size(), expired, retried, lost, batch.leaseReclaimed());
  }

  private Outcome process(TimeoutReservation timeout) {
    try {
      Optional<ResourceOperation> found =
          resource.findOperation(timeout.slotId(), timeout.releaseOperationId());
      if (found.isPresent() && !matches(found.orElseThrow(), timeout)) {
        meters.counter("venueflow.booking.expiration.mismatch").increment();
        return retry(timeout, "OPERATION_MISMATCH");
      }
      if (found.isEmpty() && !releaseAndProve(timeout)) {
        return retry(timeout, "RELEASE_UNPROVEN");
      }
      if (bookings.completeExpiration(timeout)) return Outcome.EXPIRED;
      return Outcome.LOST;
    } catch (BookingException exception) {
      return retry(timeout, "RESOURCE_UNAVAILABLE");
    } catch (RuntimeException exception) {
      return retry(timeout, "PERSISTENCE_FAILURE");
    }
  }

  private boolean releaseAndProve(TimeoutReservation timeout) {
    try {
      resource.release(timeout.slotId(), timeout.releaseOperationId(), timeout.quantity());
      meters.counter("venueflow.booking.expiration.release", "outcome", "SUCCEEDED").increment();
      return true;
    } catch (BookingException exception) {
      try {
        Optional<ResourceOperation> found =
            resource.findOperation(timeout.slotId(), timeout.releaseOperationId());
        if (found.isPresent() && matches(found.orElseThrow(), timeout)) {
          meters.counter("venueflow.booking.expiration.release", "outcome", "PROVEN").increment();
          return true;
        }
      } catch (BookingException ignored) {
        // Retry retains the durable timeout facts.
      }
      meters.counter("venueflow.booking.expiration.release", "outcome", "UNKNOWN").increment();
      return false;
    }
  }

  private Outcome retry(TimeoutReservation timeout, String code) {
    boolean updated;
    LocalDateTime now = LocalDateTime.now();
    if (timeout.attemptCount() >= properties.maxAttempts()) {
      updated = expirations.exhaust(timeout, code, now);
    } else {
      updated = expirations.retry(timeout, code, now.plus(backoff(timeout.attemptCount())), now);
    }
    return updated ? Outcome.RETRIED : Outcome.LOST;
  }

  private Duration backoff(int attempt) {
    long multiplier = 1L << Math.min(Math.max(0, attempt - 1), 20);
    Duration candidate = properties.initialBackoff().multipliedBy(multiplier);
    return candidate.compareTo(properties.maxBackoff()) > 0 ? properties.maxBackoff() : candidate;
  }

  private static boolean matches(ResourceOperation operation, TimeoutReservation timeout) {
    return timeout.releaseOperationId().equals(operation.operationId())
        && "RELEASE".equals(operation.operationType())
        && timeout.quantity() == operation.quantity();
  }

  private void updateBacklog() {
    LocalDateTime now = LocalDateTime.now();
    dueDepth.set(expirations.dueCount(now));
    oldestAge.set(expirations.oldestDueAgeSeconds(now));
  }

  private enum Outcome {
    EXPIRED,
    RETRIED,
    LOST
  }
}
