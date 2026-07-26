package com.yanerdan.venueflow.booking.reconciliation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanerdan.venueflow.booking.application.BookingErrorCode;
import com.yanerdan.venueflow.booking.application.BookingException;
import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient;
import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient.ResourceOperation;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import com.yanerdan.venueflow.booking.persistence.BookingRepository;
import com.yanerdan.venueflow.booking.reconciliation.application.port.ReconciliationIntentRepository;
import com.yanerdan.venueflow.booking.reconciliation.application.port.ReconciliationIntentRepository.ClaimedIntents;
import com.yanerdan.venueflow.booking.reconciliation.config.ReconciliationProperties;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntentState;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationOutcomeCode;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationWorkflowType;
import com.yanerdan.venueflow.booking.reconciliation.persistence.ReconciliationAuditRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {
  @Mock private ReconciliationIntentRepository intents;
  @Mock private ReconciliationAuditRepository audit;
  @Mock private BookingRepository bookings;
  @Mock private ResourceCapacityClient resource;
  private ReconciliationService service;

  @BeforeEach
  void setUp() {
    service =
        new ReconciliationService(
            intents, audit, bookings, resource, properties(), new SimpleMeterRegistry());
    when(audit.startRun(anyString(), anyString(), anyString(), any(), any())).thenReturn(10L);
  }

  @Test
  void releasesProvenOrphanAllocation() {
    ReconciliationIntent intent = intent(ReconciliationWorkflowType.ALLOCATE, null);
    when(intents.claimDue(any(), anyInt(), anyString(), any()))
        .thenReturn(new ClaimedIntents(List.of(intent), 0));
    when(resource.findOperation(2L, "allocate:request-1"))
        .thenReturn(Optional.of(new ResourceOperation("allocate:request-1", "ALLOCATE", 1)));
    when(audit.startRepair(anyLong(), eq(10L), eq(1), anyString(), anyString(), anyString()))
        .thenReturn(11L);
    when(intents.resolve(
            eq(1L), eq(1L), anyString(), eq(ReconciliationOutcomeCode.ORPHAN_RELEASED), any()))
        .thenReturn(true);

    ReconciliationSummary result = service.runOnce("SCHEDULED", null);

    assertThat(result.repaired()).isEqualTo(1);
    verify(resource).release(2L, "release:request-1", 1);
    verify(audit).completeRepair(11L, "SUCCEEDED", "RELEASE_CONFIRMED");
  }

  @Test
  void completesCancellationOnlyAfterReleaseIsProven() {
    ReconciliationIntent intent = intent(ReconciliationWorkflowType.RELEASE, 1L);
    when(intents.claimDue(any(), anyInt(), anyString(), any()))
        .thenReturn(new ClaimedIntents(List.of(intent), 0));
    when(bookings.findById(1L)).thenReturn(booking(BookingStatus.CONFIRMED));
    when(resource.findOperation(2L, "release:request-1"))
        .thenReturn(Optional.of(new ResourceOperation("release:request-1", "RELEASE", 1)));
    when(bookings.completeReconciledCancellation(eq(intent), anyString()))
        .thenReturn(ReconciliationOutcomeCode.CANCELLATION_COMPLETED);

    ReconciliationSummary result = service.runOnce("OPERATOR", "repair");

    assertThat(result.repaired()).isEqualTo(1);
    verify(bookings).completeReconciledCancellation(eq(intent), anyString());
  }

  @Test
  void resourceOutageSchedulesBoundedRetry() {
    ReconciliationIntent intent = intent(ReconciliationWorkflowType.ALLOCATE, null);
    when(intents.claimDue(any(), anyInt(), anyString(), any()))
        .thenReturn(new ClaimedIntents(List.of(intent), 0));
    when(resource.findOperation(2L, "allocate:request-1"))
        .thenThrow(
            new BookingException(
                BookingErrorCode.BOOKING_DOWNSTREAM_UNAVAILABLE, "resource unavailable"));
    when(intents.scheduleRetry(eq(1L), eq(1L), eq("lease"), anyString(), any(), any()))
        .thenReturn(true);

    ReconciliationSummary result = service.runOnce("SCHEDULED", null);

    assertThat(result.unresolved()).isEqualTo(1);
    verify(audit).recordIssue(1L, "RESOURCE_UNAVAILABLE", "ERROR");
  }

  private static ReconciliationIntent intent(ReconciliationWorkflowType workflow, Long bookingId) {
    LocalDateTime now = LocalDateTime.of(2026, 7, 26, 12, 0);
    return new ReconciliationIntent(
        1L,
        workflow,
        "request-1",
        bookingId,
        2L,
        1,
        "allocate:request-1",
        "release:request-1",
        ReconciliationIntentState.LEASED,
        1,
        1L,
        "lease",
        now,
        now);
  }

  private static BookingReservation booking(BookingStatus status) {
    LocalDateTime now = LocalDateTime.of(2026, 7, 26, 12, 0);
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
        0L,
        now,
        now,
        null,
        now);
  }

  private static ReconciliationProperties properties() {
    return new ReconciliationProperties(
        false,
        20,
        Duration.ofSeconds(30),
        Duration.ofSeconds(10),
        3,
        Duration.ofSeconds(1),
        Duration.ofMinutes(1),
        Duration.ofSeconds(3),
        Duration.ofSeconds(1),
        Duration.ofSeconds(5));
  }
}
