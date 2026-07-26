package com.yanerdan.venueflow.booking.application;

import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient;
import com.yanerdan.venueflow.booking.collaboration.UserEligibilityClient;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import com.yanerdan.venueflow.booking.persistence.BookingRepository;
import com.yanerdan.venueflow.booking.persistence.ClaimResult;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationOutcomeCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("persistence")
public class BookingReservationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BookingReservationService.class);
  private final BookingRepository repository;
  private final UserEligibilityClient userClient;
  private final ResourceCapacityClient resourceClient;
  private final Duration confirmationWindow;

  @Autowired
  public BookingReservationService(
      BookingRepository repository,
      UserEligibilityClient userClient,
      ResourceCapacityClient resourceClient,
      @Value("${venueflow.booking.confirmation-window:PT15M}") Duration confirmationWindow) {
    this.repository = repository;
    this.userClient = userClient;
    this.resourceClient = resourceClient;
    this.confirmationWindow = confirmationWindow;
  }

  BookingReservationService(
      BookingRepository repository,
      UserEligibilityClient userClient,
      ResourceCapacityClient resourceClient) {
    this(repository, userClient, resourceClient, Duration.ofMinutes(15));
  }

  public CreateResult create(String key, long userId, long slotId, int quantity) {
    validateKey(key);
    String hash = hash(userId, slotId, quantity);
    ClaimResult claim = repository.claim(userId, key, hash, slotId, quantity);
    switch (claim.kind()) {
      case SUCCEEDED:
        return new CreateResult(claim.reservation(), true);
      case CONFLICT:
        throw error(BookingErrorCode.BOOKING_IDEMPOTENCY_CONFLICT, "Idempotency key conflict");
      case PROCESSING:
        throw error(BookingErrorCode.BOOKING_REQUEST_IN_PROGRESS, "Booking request is processing");
      case FAILED:
        throw error(parseFailure(claim.failureCode()), "Booking request previously failed");
      case OWNER:
        break;
    }

    String allocationId = "allocate:" + claim.requestId();
    String releaseId = "release:" + claim.requestId();
    try {
      if (!userClient.isBookingPermitted(userId)) {
        throw error(BookingErrorCode.BOOKING_USER_NOT_ELIGIBLE, "User is not eligible");
      }
    } catch (BookingException exception) {
      repository.fail(
          claim.requestId(), exception.getCode().name(), ReconciliationOutcomeCode.NO_ALLOCATION);
      throw exception;
    }
    try {
      resourceClient.allocate(slotId, allocationId, quantity);
    } catch (BookingException exception) {
      repository.fail(
          claim.requestId(),
          exception.getCode().name(),
          exception.getCode() == BookingErrorCode.BOOKING_CAPACITY_UNAVAILABLE
              ? ReconciliationOutcomeCode.NO_ALLOCATION
              : null);
      throw exception;
    }

    try {
      return new CreateResult(
          repository.complete(
              claim.requestId(),
              userId,
              slotId,
              quantity,
              allocationId,
              releaseId,
              LocalDateTime.now().plus(confirmationWindow)),
          false);
    } catch (RuntimeException persistenceFailure) {
      try {
        resourceClient.release(slotId, releaseId, quantity);
      } catch (BookingException compensationFailure) {
        repository.fail(
            claim.requestId(), BookingErrorCode.BOOKING_COMPENSATION_REQUIRED.name(), null);
        LOGGER.error(
            "Booking compensation required. requestId={}, operationId={}, code={}",
            claim.requestId(),
            allocationId,
            BookingErrorCode.BOOKING_COMPENSATION_REQUIRED,
            compensationFailure);
        throw error(
            BookingErrorCode.BOOKING_COMPENSATION_REQUIRED,
            "Booking compensation requires operator attention",
            compensationFailure);
      }
      repository.fail(
          claim.requestId(),
          BookingErrorCode.BOOKING_PERSISTENCE_FAILED.name(),
          ReconciliationOutcomeCode.ORPHAN_RELEASED);
      throw error(
          BookingErrorCode.BOOKING_PERSISTENCE_FAILED,
          "Booking persistence failed after allocation",
          persistenceFailure);
    }
  }

  public BookingReservation get(String bookingNo) {
    BookingReservation reservation = repository.find(bookingNo);
    if (reservation == null) throw error(BookingErrorCode.BOOKING_NOT_FOUND, "Booking not found");
    return reservation;
  }

  public BookingReservation cancel(String bookingNo) {
    BookingReservation reservation = get(bookingNo);
    if (reservation.status() == BookingStatus.CANCELLED) return reservation;
    if (reservation.status() == BookingStatus.EXPIRED) {
      throw error(BookingErrorCode.BOOKING_STATE_CONFLICT, "Expired booking is terminal");
    }
    if (repository.hasLiveTimeoutLease(bookingNo)) {
      throw error(BookingErrorCode.BOOKING_TIMEOUT_IN_PROGRESS, "Timeout owns this reservation");
    }
    repository.prepareCancellation(reservation);
    resourceClient.release(
        reservation.slotId(), reservation.releaseOperationId(), reservation.quantity());
    if (!repository.cancelAndResolve(bookingNo, reservation.version())) {
      BookingReservation current = get(bookingNo);
      if (current.status() == BookingStatus.CANCELLED) return current;
      throw error(BookingErrorCode.BOOKING_STATE_CONFLICT, "Booking state changed concurrently");
    }
    return get(bookingNo);
  }

  public BookingReservation confirm(String bookingNo) {
    BookingReservation current = get(bookingNo);
    if (current.status() == BookingStatus.CONFIRMED) return current;
    if (current.status() != BookingStatus.PENDING_CONFIRMATION) {
      throw error(BookingErrorCode.BOOKING_STATE_CONFLICT, "Booking cannot be confirmed");
    }
    if (current.expireAt() == null || !LocalDateTime.now().isBefore(current.expireAt())) {
      throw error(
          BookingErrorCode.BOOKING_CONFIRMATION_DEADLINE_EXPIRED,
          "Booking confirmation deadline expired");
    }
    if (repository.hasLiveTimeoutLease(bookingNo)) {
      throw error(BookingErrorCode.BOOKING_TIMEOUT_IN_PROGRESS, "Timeout owns this reservation");
    }
    try {
      BookingReservation result = repository.confirm(bookingNo);
      if (result != null && result.status() == BookingStatus.CONFIRMED) return result;
    } catch (IllegalArgumentException exception) {
      throw error(
          BookingErrorCode.BOOKING_CONFIRMATION_DEADLINE_EXPIRED,
          "Booking confirmation deadline expired");
    }
    throw error(BookingErrorCode.BOOKING_STATE_CONFLICT, "Booking state changed concurrently");
  }

  private static void validateKey(String key) {
    try {
      UUID.fromString(key);
    } catch (RuntimeException exception) {
      throw error(BookingErrorCode.BOOKING_VALIDATION_FAILED, "Invalid Idempotency-Key");
    }
  }

  static String hash(long userId, long slotId, int quantity) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(
                      (userId + "|" + slotId + "|" + quantity).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required", exception);
    }
  }

  private static BookingErrorCode parseFailure(String code) {
    try {
      return BookingErrorCode.valueOf(code);
    } catch (RuntimeException exception) {
      return BookingErrorCode.BOOKING_PERSISTENCE_FAILED;
    }
  }

  private static BookingException error(BookingErrorCode code, String message) {
    return new BookingException(code, message);
  }

  private static BookingException error(BookingErrorCode code, String message, Throwable cause) {
    return new BookingException(code, message, cause);
  }

  public record CreateResult(BookingReservation reservation, boolean replay) {}
}
