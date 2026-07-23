package com.yanerdan.venueflow.booking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.time.LocalDateTime;
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
    when(repository.claim(1L, KEY, hash())).thenReturn(owner());
    when(userClient.isBookingPermitted(1L)).thenReturn(true);
    when(repository.complete("request-1", 1L, 2L, 1, "allocate:request-1", "release:request-1"))
        .thenReturn(reservation(BookingStatus.CONFIRMED));

    assertThat(service.create(KEY, 1L, 2L, 1).replay()).isFalse();
    verify(resourceClient).allocate(2L, "allocate:request-1", 1);

    when(repository.claim(1L, KEY, hash()))
        .thenReturn(
            new ClaimResult(
                ClaimResult.Kind.SUCCEEDED,
                "request-1",
                reservation(BookingStatus.CONFIRMED),
                null));
    assertThat(service.create(KEY, 1L, 2L, 1).replay()).isTrue();
  }

  @Test
  void rejectsConflictBeforeCollaboratorCalls() {
    when(repository.claim(1L, KEY, hash()))
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
    when(repository.claim(1L, KEY, hash())).thenReturn(owner());
    when(userClient.isBookingPermitted(1L)).thenReturn(true);
    when(repository.complete("request-1", 1L, 2L, 1, "allocate:request-1", "release:request-1"))
        .thenThrow(new IllegalStateException("database unavailable"));

    assertThatThrownBy(() -> service.create(KEY, 1L, 2L, 1))
        .isInstanceOfSatisfying(
            BookingException.class,
            exception ->
                assertThat(exception.getCode())
                    .isEqualTo(BookingErrorCode.BOOKING_PERSISTENCE_FAILED));
    verify(resourceClient).release(2L, "release:request-1", 1);
  }

  @Test
  void reportsCompensationRequiredWhenReleaseFails() {
    when(repository.claim(1L, KEY, hash())).thenReturn(owner());
    when(userClient.isBookingPermitted(1L)).thenReturn(true);
    when(repository.complete("request-1", 1L, 2L, 1, "allocate:request-1", "release:request-1"))
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
    when(repository.cancel("booking-1", 0L)).thenReturn(true);

    assertThat(service.cancel("booking-1").status()).isEqualTo(BookingStatus.CANCELLED);
    verify(resourceClient).release(2L, "release:request-1", 1);
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

  private static String hash() {
    return BookingReservationService.hash(1L, 2L, 1);
  }
}
