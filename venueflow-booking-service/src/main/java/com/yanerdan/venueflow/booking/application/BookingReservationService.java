package com.yanerdan.venueflow.booking.application;

import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient;
import com.yanerdan.venueflow.booking.collaboration.UserEligibilityClient;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import com.yanerdan.venueflow.booking.persistence.BookingRepository;
import com.yanerdan.venueflow.booking.persistence.ClaimResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("persistence")
public class BookingReservationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BookingReservationService.class);
  private final BookingRepository repository;
  private final UserEligibilityClient userClient;
  private final ResourceCapacityClient resourceClient;

  public BookingReservationService(
      BookingRepository repository,
      UserEligibilityClient userClient,
      ResourceCapacityClient resourceClient) {
    this.repository = repository;
    this.userClient = userClient;
    this.resourceClient = resourceClient;
  }

  public CreateResult create(String key, long userId, long slotId, int quantity) {
    validateKey(key);
    String hash = hash(userId, slotId, quantity);
    ClaimResult claim = repository.claim(userId, key, hash);
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
      resourceClient.allocate(slotId, allocationId, quantity);
    } catch (BookingException exception) {
      repository.fail(claim.requestId(), exception.getCode().name());
      throw exception;
    }

    try {
      return new CreateResult(
          repository.complete(claim.requestId(), userId, slotId, quantity, allocationId, releaseId),
          false);
    } catch (RuntimeException persistenceFailure) {
      try {
        resourceClient.release(slotId, releaseId, quantity);
      } catch (BookingException compensationFailure) {
        repository.fail(claim.requestId(), BookingErrorCode.BOOKING_COMPENSATION_REQUIRED.name());
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
      repository.fail(claim.requestId(), BookingErrorCode.BOOKING_PERSISTENCE_FAILED.name());
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
    resourceClient.release(
        reservation.slotId(), reservation.releaseOperationId(), reservation.quantity());
    if (!repository.cancel(bookingNo, reservation.version())) {
      BookingReservation current = get(bookingNo);
      if (current.status() == BookingStatus.CANCELLED) return current;
      throw error(BookingErrorCode.BOOKING_STATE_CONFLICT, "Booking state changed concurrently");
    }
    return get(bookingNo);
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
