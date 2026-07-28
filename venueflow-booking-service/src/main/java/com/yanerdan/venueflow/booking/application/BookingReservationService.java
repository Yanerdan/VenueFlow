package com.yanerdan.venueflow.booking.application;

import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient;
import com.yanerdan.venueflow.booking.collaboration.UserEligibilityClient;
import com.yanerdan.venueflow.booking.domain.BookingApplicationDetails;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import com.yanerdan.venueflow.booking.persistence.BookingApprovalAction;
import com.yanerdan.venueflow.booking.persistence.BookingRepository;
import com.yanerdan.venueflow.booking.persistence.BookingRepository.BookingHistoryPage;
import com.yanerdan.venueflow.booking.persistence.ClaimResult;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationOutcomeCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
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
  private final Duration checkInEarlyWindow;
  private final Duration checkInLateWindow;
  private final Clock clock;

  @Autowired
  public BookingReservationService(
      BookingRepository repository,
      UserEligibilityClient userClient,
      ResourceCapacityClient resourceClient,
      @Value("${venueflow.booking.confirmation-window:PT15M}") Duration confirmationWindow,
      @Value("${venueflow.booking.check-in-early-window:PT30M}") Duration checkInEarlyWindow,
      @Value("${venueflow.booking.check-in-late-window:PT30M}") Duration checkInLateWindow) {
    this(
        repository,
        userClient,
        resourceClient,
        confirmationWindow,
        checkInEarlyWindow,
        checkInLateWindow,
        Clock.systemUTC());
  }

  BookingReservationService(
      BookingRepository repository,
      UserEligibilityClient userClient,
      ResourceCapacityClient resourceClient,
      Duration confirmationWindow,
      Duration checkInEarlyWindow,
      Duration checkInLateWindow,
      Clock clock) {
    this.repository = repository;
    this.userClient = userClient;
    this.resourceClient = resourceClient;
    this.confirmationWindow = confirmationWindow;
    this.checkInEarlyWindow = boundedWindow(checkInEarlyWindow, "check-in early window");
    this.checkInLateWindow = boundedWindow(checkInLateWindow, "check-in late window");
    this.clock = clock;
  }

  BookingReservationService(
      BookingRepository repository,
      UserEligibilityClient userClient,
      ResourceCapacityClient resourceClient) {
    this(
        repository,
        userClient,
        resourceClient,
        Duration.ofMinutes(15),
        Duration.ofMinutes(30),
        Duration.ofMinutes(30),
        Clock.systemUTC());
  }

  public CreateResult create(String key, long userId, long slotId, int quantity) {
    validateKey(key);
    return createOwned(
        key,
        userId,
        slotId,
        quantity,
        hash(userId, slotId, quantity),
        BookingApplicationDetails.historical());
  }

  public CreateResult create(
      String key,
      long userId,
      long slotId,
      int quantity,
      String activityTitle,
      String purpose,
      String contactName,
      String contactPhone,
      String note) {
    validateKey(key);
    BookingApplicationDetails details =
        new BookingApplicationDetails(activityTitle, purpose, contactName, contactPhone, note);
    return createOwned(
        key, userId, slotId, quantity, hash(userId, slotId, quantity, details), details);
  }

  private CreateResult createOwned(
      String key,
      long userId,
      long slotId,
      int quantity,
      String hash,
      BookingApplicationDetails details) {
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
    ResourceCapacityClient.ResourceSlot responsibility;
    try {
      responsibility = resourceClient.findSlot(slotId);
    } catch (BookingException exception) {
      repository.fail(
          claim.requestId(), exception.getCode().name(), ReconciliationOutcomeCode.NO_ALLOCATION);
      throw exception;
    }
    try {
      validateBookingRules(responsibility);
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
      LocalDateTime expireAt = LocalDateTime.now().plus(confirmationWindow);
      BookingReservation completed =
          responsibility == null
              ? (details.activityTitle() == null
                  ? repository.complete(
                      claim.requestId(),
                      userId,
                      slotId,
                      quantity,
                      allocationId,
                      releaseId,
                      expireAt)
                  : repository.complete(
                      claim.requestId(),
                      userId,
                      slotId,
                      quantity,
                      allocationId,
                      releaseId,
                      expireAt,
                      details))
              : repository.complete(
                  claim.requestId(),
                  userId,
                  slotId,
                  quantity,
                  allocationId,
                  releaseId,
                  expireAt,
                  details,
                  responsibility.resourceId(),
                  responsibility.ownerDepartment(),
                  responsibility.approverExternalUserId(),
                  responsibility.approvalMode(),
                  responsibility.finalApproverExternalUserId());
      return new CreateResult(completed, false);
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

  private void validateBookingRules(ResourceCapacityClient.ResourceSlot slot) {
    if (slot == null) {
      return;
    }
    int minimumHours = slot.minAdvanceHours() == null ? 0 : slot.minAdvanceHours();
    int maximumDays = slot.maxAdvanceDays() == null ? 90 : slot.maxAdvanceDays();
    int maximumMinutes = slot.maxDurationMinutes() == null ? 480 : slot.maxDurationMinutes();
    Instant now = clock.instant();
    boolean tooSoon = slot.startAt().isBefore(now.plus(Duration.ofHours(minimumHours)));
    boolean tooFar = slot.startAt().isAfter(now.plus(Duration.ofDays(maximumDays)));
    boolean tooLong =
        Duration.between(slot.startAt(), slot.endAt()).compareTo(Duration.ofMinutes(maximumMinutes))
            > 0;
    if (tooSoon || tooFar || tooLong) {
      throw error(
          BookingErrorCode.BOOKING_VALIDATION_FAILED,
          "Booking violates resource advance or duration rules");
    }
  }

  public BookingReservation get(String bookingNo) {
    BookingReservation reservation = repository.find(bookingNo);
    if (reservation == null) throw error(BookingErrorCode.BOOKING_NOT_FOUND, "Booking not found");
    return reservation;
  }

  public BookingHistoryPage history(long userId, int pageNumber, int pageSize) {
    if (userId <= 0 || pageNumber < 0 || pageSize < 1 || pageSize > 100) {
      throw error(BookingErrorCode.BOOKING_VALIDATION_FAILED, "Invalid history page");
    }
    return repository.history(userId, pageNumber, pageSize);
  }

  public BookingHistoryPage managementHistory(BookingStatus status, int pageNumber, int pageSize) {
    if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
      throw error(BookingErrorCode.BOOKING_VALIDATION_FAILED, "Invalid management page");
    }
    return repository.managementHistory(status, pageNumber, pageSize);
  }

  public BookingHistoryPage managementHistory(
      BookingStatus status, String trustedUserId, String role, int pageNumber, int pageSize) {
    if ("SYSTEM_ADMIN".equals(role) || trustedUserId == null) {
      return managementHistory(status, pageNumber, pageSize);
    }
    if (trustedUserId.isBlank() || pageNumber < 0 || pageSize < 1 || pageSize > 100) {
      throw error(BookingErrorCode.BOOKING_VALIDATION_FAILED, "Invalid management scope");
    }
    return repository.managementHistory(status, trustedUserId, pageNumber, pageSize);
  }

  public void requireApprovalScope(String bookingNo, String trustedUserId, String role) {
    if ("SYSTEM_ADMIN".equals(role) || trustedUserId == null) {
      return;
    }
    BookingReservation reservation = get(bookingNo);
    String currentApprover =
        reservation.currentApprovalStep() == 2
            ? reservation.finalAssignedApproverExternalUserId()
            : reservation.assignedApproverExternalUserId();
    if (currentApprover == null || !currentApprover.equals(trustedUserId)) {
      throw error(BookingErrorCode.BOOKING_FORBIDDEN, "Booking is assigned to another approver");
    }
  }

  public BookingReservation cancel(String bookingNo) {
    return cancelInternal(bookingNo, "USER_CANCELLED", "CANCELLED", null, "APPLICANT", true);
  }

  public BookingReservation cancel(String bookingNo, String note) {
    return cancelInternal(
        bookingNo, "USER_CANCELLED", "CANCELLED", normalizeOptional(note), "APPLICANT", false);
  }

  public BookingReservation reject(String bookingNo, String reason, String reviewerRole) {
    return reject(bookingNo, reason, reviewerRole, null);
  }

  public BookingReservation reject(
      String bookingNo, String reason, String reviewerRole, String actorExternalUserId) {
    if (reason == null || reason.isBlank()) {
      throw error(BookingErrorCode.BOOKING_VALIDATION_FAILED, "Rejection reason is required");
    }
    BookingReservation result =
        cancelInternal(
            bookingNo,
            "MANAGEMENT_REJECTED",
            "REJECTED",
            normalizeRequired(reason),
            reviewerRole,
            false);
    repository.recordApprovalAction(
        bookingNo,
        result.currentApprovalStep(),
        actorExternalUserId,
        reviewerRole,
        "REJECTED",
        normalizeRequired(reason));
    return result;
  }

  private BookingReservation cancelInternal(
      String bookingNo,
      String terminalReason,
      String decision,
      String note,
      String reviewerRole,
      boolean legacyAction) {
    BookingReservation reservation = get(bookingNo);
    if (reservation.status() == BookingStatus.CANCELLED) return reservation;
    if (reservation.status() != BookingStatus.PENDING_CONFIRMATION
        && reservation.status() != BookingStatus.CONFIRMED) {
      throw error(BookingErrorCode.BOOKING_STATE_CONFLICT, "Booking is terminal");
    }
    if (repository.hasLiveTimeoutLease(bookingNo)) {
      throw error(BookingErrorCode.BOOKING_TIMEOUT_IN_PROGRESS, "Timeout owns this reservation");
    }
    repository.prepareCancellation(reservation);
    resourceClient.release(
        reservation.slotId(), reservation.releaseOperationId(), reservation.quantity());
    boolean cancelled =
        legacyAction
            ? repository.cancelAndResolve(bookingNo, reservation.version())
            : repository.cancelAndResolve(
                bookingNo, reservation.version(), terminalReason, decision, note, reviewerRole);
    if (!cancelled) {
      BookingReservation current = get(bookingNo);
      if (current.status() == BookingStatus.CANCELLED) return current;
      throw error(BookingErrorCode.BOOKING_STATE_CONFLICT, "Booking state changed concurrently");
    }
    return get(bookingNo);
  }

  public BookingReservation confirm(String bookingNo) {
    return confirmInternal(bookingNo, null, null, true);
  }

  public BookingReservation confirm(String bookingNo, String note, String reviewerRole) {
    return confirm(bookingNo, note, reviewerRole, null);
  }

  public BookingReservation confirm(
      String bookingNo, String note, String reviewerRole, String actorExternalUserId) {
    return confirmInternal(bookingNo, note, reviewerRole, actorExternalUserId, false);
  }

  private BookingReservation confirmInternal(
      String bookingNo, String note, String reviewerRole, boolean legacyAction) {
    return confirmInternal(bookingNo, note, reviewerRole, null, legacyAction);
  }

  private BookingReservation confirmInternal(
      String bookingNo,
      String note,
      String reviewerRole,
      String actorExternalUserId,
      boolean legacyAction) {
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
      BookingReservation result =
          legacyAction
              ? repository.confirm(bookingNo)
              : repository.confirm(
                  bookingNo, normalizeOptional(note), reviewerRole, actorExternalUserId);
      if (result != null
          && (result.status() == BookingStatus.CONFIRMED
              || (result.status() == BookingStatus.PENDING_CONFIRMATION
                  && result.currentApprovalStep() == 2))) {
        return result;
      }
    } catch (IllegalArgumentException exception) {
      throw error(
          BookingErrorCode.BOOKING_CONFIRMATION_DEADLINE_EXPIRED,
          "Booking confirmation deadline expired");
    }
    throw error(BookingErrorCode.BOOKING_STATE_CONFLICT, "Booking state changed concurrently");
  }

  public List<BookingApprovalAction> approvalActions(String bookingNo) {
    get(bookingNo);
    return repository.approvalActions(bookingNo);
  }

  public BookingReservation checkIn(String bookingNo) {
    BookingReservation current = get(bookingNo);
    if (current.status() == BookingStatus.COMPLETED) return current;
    if (current.status() != BookingStatus.CONFIRMED) {
      throw error(BookingErrorCode.BOOKING_STATE_CONFLICT, "Booking cannot be checked in");
    }
    ResourceCapacityClient.ResourceSlot slot = resourceClient.findSlot(current.slotId());
    Instant now = clock.instant();
    Instant opensAt = slot.startAt().minus(checkInEarlyWindow);
    Instant closesAt = slot.endAt().plus(checkInLateWindow);
    if (now.isBefore(opensAt) || now.isAfter(closesAt)) {
      throw error(
          BookingErrorCode.BOOKING_CHECK_IN_WINDOW_INVALID,
          "Booking is outside the check-in window");
    }
    LocalDateTime completedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
    if (repository.completeCheckIn(bookingNo, current.version(), completedAt)) {
      return get(bookingNo);
    }
    BookingReservation latest = get(bookingNo);
    if (latest.status() == BookingStatus.COMPLETED) return latest;
    throw error(BookingErrorCode.BOOKING_STATE_CONFLICT, "Booking state changed concurrently");
  }

  private static Duration boundedWindow(Duration value, String name) {
    if (value == null || value.isNegative() || value.compareTo(Duration.ofHours(24)) > 0) {
      throw new IllegalArgumentException(name + " must be between PT0S and PT24H");
    }
    return value;
  }

  private static String normalizeRequired(String value) {
    String normalized = value.trim();
    if (normalized.length() > 1000) {
      throw error(BookingErrorCode.BOOKING_VALIDATION_FAILED, "Review note is too long");
    }
    return normalized;
  }

  private static String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : normalizeRequired(value);
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

  static String hash(long userId, long slotId, int quantity, BookingApplicationDetails details) {
    return sha256(
        userId
            + "|"
            + slotId
            + "|"
            + quantity
            + "|"
            + details.activityTitle()
            + "|"
            + details.purpose()
            + "|"
            + details.contactName()
            + "|"
            + details.contactPhone()
            + "|"
            + (details.note() == null ? "" : details.note()));
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
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
