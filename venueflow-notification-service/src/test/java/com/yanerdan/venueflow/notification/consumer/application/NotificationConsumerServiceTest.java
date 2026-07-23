package com.yanerdan.venueflow.notification.consumer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanerdan.venueflow.notification.consumer.domain.BookingEvent;
import com.yanerdan.venueflow.notification.consumer.domain.ConsumedIdentity;
import com.yanerdan.venueflow.notification.consumer.domain.ConsumptionResult;
import com.yanerdan.venueflow.notification.consumer.domain.IdentityCollisionException;
import com.yanerdan.venueflow.notification.consumer.persistence.NotificationConsumerRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NotificationConsumerServiceTest {
  private final NotificationConsumerRepository repository =
      Mockito.mock(NotificationConsumerRepository.class);
  private final NotificationConsumerService service =
      new NotificationConsumerService(
          repository, Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC));

  @Test
  void insertsConsumedIdentityAndNotificationOnce() {
    BookingEvent event = event("hash");
    when(repository.findConsumed("consumer", event.eventId())).thenReturn(Optional.empty());

    assertThat(service.consume("consumer", event)).isEqualTo(ConsumptionResult.CONSUMED);
    verify(repository).insertConsumed(eq("consumer"), eq(event), any());
    verify(repository).insertNotification(eq("consumer"), eq(event), any(), any());
  }

  @Test
  void exactDuplicateCreatesNoSecondNotification() {
    BookingEvent event = event("hash");
    when(repository.findConsumed("consumer", event.eventId()))
        .thenReturn(Optional.of(new ConsumedIdentity(event.eventType(), 1, "hash")));

    assertThat(service.consume("consumer", event)).isEqualTo(ConsumptionResult.DUPLICATE);
    verify(repository, never()).insertNotification(any(), any(), any(), any());
  }

  @Test
  void changedHashIsIdentityCollision() {
    BookingEvent event = event("new-hash");
    when(repository.findConsumed("consumer", event.eventId()))
        .thenReturn(Optional.of(new ConsumedIdentity(event.eventType(), 1, "old-hash")));

    assertThatThrownBy(() -> service.consume("consumer", event))
        .isInstanceOf(IdentityCollisionException.class);
  }

  private static BookingEvent event(String hash) {
    return new BookingEvent(
        "c88d6348-92c5-4ead-b14a-22557b60610d",
        "booking.reservation.confirmed",
        1,
        Instant.parse("2026-07-23T10:00:00Z"),
        "venueflow-booking-service",
        "BOOKING",
        "B-1",
        null,
        "B-1",
        1,
        2,
        1,
        "CONFIRMED",
        "booking.reservation.confirmed.v1",
        hash);
  }
}
