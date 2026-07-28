package com.yanerdan.venueflow.booking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient;
import com.yanerdan.venueflow.booking.collaboration.UserEligibilityClient;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import com.yanerdan.venueflow.booking.persistence.BookingRepository;
import com.yanerdan.venueflow.booking.persistence.ClaimResult;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationOutcomeCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingReservationServiceTest {
  private static final String KEY = "f4f4266a-b145-44f4-a375-0d59450f5147";
  @Mock private BookingRepository repository;
  @Mock private UserEligibilityClient userClient;
  @Mock private ResourceCapacityClient resourceClient;
  private BookingReservationService service;

  @BeforeEach
  void setUp() {
    service = new BookingReservationService(repository, userClient, resourceClient);
  }

  @Test
  void createsOnceAndReturnsSucceededReplayWithoutCollaboratorCalls() {
    when(repository.claim(1L, KEY, hash(), 2L, 1)).thenReturn(owner());
    when(userClient.isBookingPermitted(1L)).thenReturn(true);
    when(repository.complete(
            eq("request-1"),
            eq(1L),
            eq(2L),
            eq(1),
            eq("allocate:request-1"),
            eq("release:request-1"),
            any(LocalDateTime.class)))
        .thenReturn(reservation(BookingStatus.PENDING_CONFIRMATION));

    assertThat(service.create(KEY, 1L, 2L, 1).replay()).isFalse();
    verify(resourceClient).allocate(2L, "allocate:request-1", 1);

    when(repository.claim(1L, KEY, hash(), 2L, 1))
        .thenReturn(
            new ClaimResult(
                ClaimResult.Kind.SUCCEEDED,
                "request-1",
                reservation(BookingStatus.PENDING_CONFIRMATION),
                null));
    assertThat(service.create(KEY, 1L, 2L, 1).replay()).isTrue();
  }

  @Test
  void rejectsResourceRuleViolationBeforeCapacityAllocation() {
    Instant now = Instant.parse("2026-07-28T08:00:00Z");
    service =
        new BookingReservationService(
            repository,
            userClient,
            resourceClient,
            Duration.ofMinutes(15),
            Duration.ofMinutes(30),
            Duration.ofMinutes(30),
            Clock.fixed(now, ZoneOffset.UTC));
    when(repository.claim(1L, KEY, hash(), 2L, 1)).thenReturn(owner());
    when(userClient.isBookingPermitted(1L)).thenReturn(true);
    when(resourceClient.findSlot(2L))
        .thenReturn(
            new ResourceCapacityClient.ResourceSlot(
                2L,
                10L,
                "Student Affairs",
                "approver-1",
                "DIRECT",
                null,
                "Campus card required",
                24,
                30,
                60,
                now.plus(Duration.ofHours(2)),
                now.plus(Duration.ofHours(4))));

    assertThatThrownBy(() -> service.create(KEY, 1L, 2L, 1))
        .isInstanceOfSatisfying(
            BookingException.class,
            exception ->
                assertThat(exception.getCode())
                    .isEqualTo(BookingErrorCode.BOOKING_VALIDATION_FAILED));
    verify(resourceClient, never())
        .allocate(
            org.mockito.ArgumentMatchers.anyLong(),
            anyString(),
            org.mockito.ArgumentMatchers.anyInt());
    verify(repository)
        .fail(
            "request-1",
            BookingErrorCode.BOOKING_VALIDATION_FAILED.name(),
            ReconciliationOutcomeCode.NO_ALLOCATION);
  }

  @Test
  void rejectsConflictBeforeCollaboratorCalls() {
    when(repository.claim(1L, KEY, hash(), 2L, 1))
        .thenReturn(new ClaimResult(ClaimResult.Kind.CONFLICT, null, null, null));

    assertThatThrownBy(() -> service.create(KEY, 1L, 2L, 1))
        .isInstanceOfSatisfying(
            BookingException.class,
            exception ->
                assertThat(exception.getCode())
                    .isEqualTo(BookingErrorCode.BOOKING_IDEMPOTENCY_CONFLICT));
    verify(userClient, never()).isBookingPermitted(1L);
  }

  @Test
  void compensatesWhenFinalPersistenceFails() {
    when(repository.claim(1L, KEY, hash(), 2L, 1)).thenReturn(owner());
    when(userClient.isBookingPermitted(1L)).thenReturn(true);
    when(repository.complete(
            eq("request-1"),
            eq(1L),
            eq(2L),
            eq(1),
            eq("allocate:request-1"),
            eq("release:request-1"),
            any(LocalDateTime.class)))
        .thenThrow(new IllegalStateException("database unavailable"));

    assertThatThrownBy(() -> service.create(KEY, 1L, 2L, 1))
        .isInstanceOfSatisfying(
            BookingException.class,
            exception ->
                assertThat(exception.getCode())
                    .isEqualTo(BookingErrorCode.BOOKING_PERSISTENCE_FAILED));
    verify(resourceClient).release(2L, "release:request-1", 1);
    verify(repository)
        .fail(
            "request-1",
            BookingErrorCode.BOOKING_PERSISTENCE_FAILED.name(),
            ReconciliationOutcomeCode.ORPHAN_RELEASED);
  }

  @Test
  void reportsCompensationRequiredWhenReleaseFails() {
    when(repository.claim(1L, KEY, hash(), 2L, 1)).thenReturn(owner());
    when(userClient.isBookingPermitted(1L)).thenReturn(true);
    when(repository.complete(
            eq("request-1"),
            eq(1L),
            eq(2L),
            eq(1),
            eq("allocate:request-1"),
            eq("release:request-1"),
            any(LocalDateTime.class)))
        .thenThrow(new IllegalStateException("database unavailable"));
    doThrow(
            new BookingException(
                BookingErrorCode.BOOKING_DOWNSTREAM_UNAVAILABLE, "resource unavailable"))
        .when(resourceClient)
        .release(2L, "release:request-1", 1);

    assertThatThrownBy(() -> service.create(KEY, 1L, 2L, 1))
        .isInstanceOfSatisfying(
            BookingException.class,
            exception ->
                assertThat(exception.getCode())
                    .isEqualTo(BookingErrorCode.BOOKING_COMPENSATION_REQUIRED));
  }

  @Test
  void cancellationReleasesBeforeConditionalTransitionAndReplays() {
    BookingReservation confirmed = reservation(BookingStatus.CONFIRMED);
    when(repository.find("booking-1"))
        .thenReturn(confirmed)
        .thenReturn(reservation(BookingStatus.CANCELLED));
    when(repository.cancelAndResolve("booking-1", 0L)).thenReturn(true);

    assertThat(service.cancel("booking-1").status()).isEqualTo(BookingStatus.CANCELLED);
    verify(repository).prepareCancellation(confirmed);
    verify(resourceClient).release(2L, "release:request-1", 1);
  }

  @Test
  void checkInCompletesInsideWindowAndReplaysWithoutAnotherLookup() {
    Instant now = Instant.parse("2026-07-26T10:00:00Z");
    service =
        new BookingReservationService(
            repository,
            userClient,
            resourceClient,
            Duration.ofMinutes(15),
            Duration.ofMinutes(30),
            Duration.ofMinutes(30),
            Clock.fixed(now, ZoneOffset.UTC));
    BookingReservation confirmed = reservation(BookingStatus.CONFIRMED);
    BookingReservation completed = reservation(BookingStatus.COMPLETED);
    when(repository.find("booking-1")).thenReturn(confirmed).thenReturn(completed);
    when(resourceClient.findSlot(2L))
        .thenReturn(
            new ResourceCapacityClient.ResourceSlot(2L, now.minusSeconds(60), now.plusSeconds(60)));
    when(repository.completeCheckIn("booking-1", 0L, LocalDateTime.ofInstant(now, ZoneOffset.UTC)))
        .thenReturn(true);

    assertThat(service.checkIn("booking-1").status()).isEqualTo(BookingStatus.COMPLETED);
    assertThat(service.checkIn("booking-1").status()).isEqualTo(BookingStatus.COMPLETED);
    verify(resourceClient).findSlot(2L);
  }

  @Test
  void checkInOutsideWindowDoesNotWrite() {
    Instant now = Instant.parse("2026-07-26T10:00:00Z");
    service =
        new BookingReservationService(
            repository,
            userClient,
            resourceClient,
            Duration.ofMinutes(15),
            Duration.ZERO,
            Duration.ZERO,
            Clock.fixed(now, ZoneOffset.UTC));
    when(repository.find("booking-1")).thenReturn(reservation(BookingStatus.CONFIRMED));
    when(resourceClient.findSlot(2L))
        .thenReturn(
            new ResourceCapacityClient.ResourceSlot(2L, now.plusSeconds(60), now.plusSeconds(120)));

    assertThatThrownBy(() -> service.checkIn("booking-1"))
        .isInstanceOfSatisfying(
            BookingException.class,
            exception ->
                assertThat(exception.getCode())
                    .isEqualTo(BookingErrorCode.BOOKING_CHECK_IN_WINDOW_INVALID));
    verify(repository, never())
        .completeCheckIn(anyString(), org.mockito.ArgumentMatchers.anyLong(), any());
  }

  @Test
  void cancellationAndCheckInHaveOneWinner() {
    Instant now = Instant.parse("2026-07-26T10:00:00Z");
    service =
        new BookingReservationService(
            repository,
            userClient,
            resourceClient,
            Duration.ofMinutes(15),
            Duration.ofMinutes(30),
            Duration.ofMinutes(30),
            Clock.fixed(now, ZoneOffset.UTC));
    when(repository.find("booking-1"))
        .thenReturn(reservation(BookingStatus.CONFIRMED))
        .thenReturn(reservation(BookingStatus.CANCELLED));
    when(resourceClient.findSlot(2L))
        .thenReturn(
            new ResourceCapacityClient.ResourceSlot(2L, now.minusSeconds(60), now.plusSeconds(60)));
    when(repository.completeCheckIn("booking-1", 0L, LocalDateTime.ofInstant(now, ZoneOffset.UTC)))
        .thenReturn(false);

    assertThatThrownBy(() -> service.checkIn("booking-1"))
        .isInstanceOfSatisfying(
            BookingException.class,
            exception ->
                assertThat(exception.getCode()).isEqualTo(BookingErrorCode.BOOKING_STATE_CONFLICT));
  }

  @Test
  void completedReservationCannotReleaseCapacityThroughCancellation() {
    when(repository.find("booking-1")).thenReturn(reservation(BookingStatus.COMPLETED));

    assertThatThrownBy(() -> service.cancel("booking-1"))
        .isInstanceOfSatisfying(
            BookingException.class,
            exception ->
                assertThat(exception.getCode()).isEqualTo(BookingErrorCode.BOOKING_STATE_CONFLICT));
    verify(resourceClient, never())
        .release(
            org.mockito.ArgumentMatchers.anyLong(),
            anyString(),
            org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void approverManagementHistoryUsesTrustedAssignmentScope() {
    BookingRepository.BookingHistoryPage page =
        new BookingRepository.BookingHistoryPage(List.of(), 0, 0, 20);
    when(repository.managementHistory(null, "approver-uuid", 0, 20)).thenReturn(page);

    assertThat(service.managementHistory(null, "approver-uuid", "APPROVER", 0, 20)).isSameAs(page);
    verify(repository).managementHistory(null, "approver-uuid", 0, 20);
    verify(repository, never()).managementHistory(null, 0, 20);
  }

  @Test
  void finalApproverOwnsSecondStageScope() {
    when(repository.find("booking-1")).thenReturn(twoStageReservation(2));

    service.requireApprovalScope("booking-1", "final-approver", "APPROVER");

    assertThatThrownBy(
            () -> service.requireApprovalScope("booking-1", "initial-approver", "APPROVER"))
        .isInstanceOfSatisfying(
            BookingException.class,
            exception ->
                assertThat(exception.getCode()).isEqualTo(BookingErrorCode.BOOKING_FORBIDDEN));
  }

  @Test
  void initialApprovalMayAdvanceWithoutConfirming() {
    BookingReservation pending = twoStageReservation(1);
    BookingReservation advanced = twoStageReservation(2);
    when(repository.find("booking-1")).thenReturn(pending);
    when(repository.hasLiveTimeoutLease("booking-1")).thenReturn(false);
    when(repository.confirm("booking-1", "初审通过", "APPROVER", "initial-approver"))
        .thenReturn(advanced);

    assertThat(service.confirm("booking-1", "初审通过", "APPROVER", "initial-approver"))
        .extracting(BookingReservation::status, BookingReservation::currentApprovalStep)
        .containsExactly(BookingStatus.PENDING_CONFIRMATION, 2);
  }

  private static ClaimResult owner() {
    return new ClaimResult(ClaimResult.Kind.OWNER, "request-1", null, null);
  }

  private static BookingReservation reservation(BookingStatus status) {
    LocalDateTime now = LocalDateTime.of(2026, 7, 23, 12, 0);
    return new BookingReservation(
        1L,
        "booking-1",
        "request-1",
        1L,
        2L,
        1,
        status,
        "allocate:request-1",
        "release:request-1",
        status == BookingStatus.CANCELLED ? 1L : 0L,
        now,
        now,
        status == BookingStatus.CANCELLED ? now : null,
        now);
  }

  private static BookingReservation twoStageReservation(int step) {
    LocalDateTime now = LocalDateTime.now();
    return new BookingReservation(
        1L,
        "booking-1",
        "request-1",
        1L,
        2L,
        1,
        BookingStatus.PENDING_CONFIRMATION,
        "allocate:request-1",
        "release:request-1",
        step - 1L,
        now,
        now.plusMinutes(10),
        null,
        null,
        null,
        null,
        null,
        now,
        "活动",
        "用途",
        "联系人",
        "13800000000",
        null,
        null,
        null,
        null,
        null,
        9L,
        "校团委",
        "initial-approver",
        "TWO_STAGE",
        "final-approver",
        step);
  }

  private static String hash() {
    return BookingReservationService.hash(1L, 2L, 1);
  }
}
