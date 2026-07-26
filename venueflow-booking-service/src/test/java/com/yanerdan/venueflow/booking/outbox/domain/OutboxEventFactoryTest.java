package com.yanerdan.venueflow.booking.outbox.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OutboxEventFactoryTest {
  private static final Instant NOW = Instant.parse("2026-07-23T08:00:00Z");

  @Test
  void createsStableBoundedConfirmedEnvelope() throws Exception {
    OutboxEvent event =
        new OutboxEventFactory(new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC))
            .create(booking(BookingStatus.CONFIRMED));

    assertThat(event.eventId()).hasSize(36);
    assertThat(event.eventType()).isEqualTo("BOOKING_RESERVATION_CONFIRMED");
    assertThat(event.routingKey()).isEqualTo("booking.reservation.confirmed.v1");
    assertThat(event.status()).isEqualTo(OutboxStatus.NEW);
    assertThat(event.createdAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
    assertThat(new ObjectMapper().readTree(event.payload()).get("occurredAt").asText())
        .isEqualTo(NOW.toString());
    assertThat(event.payload())
        .contains("\"bookingNo\":\"B-1\"", "\"status\":\"CONFIRMED\"")
        .doesNotContain("password", "jdbc:", "rabbitmq");
  }

  @Test
  void createsVersionedCancelledEnvelope() {
    OutboxEvent event =
        new OutboxEventFactory(new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC))
            .create(booking(BookingStatus.CANCELLED));

    assertThat(event.eventType()).isEqualTo("BOOKING_RESERVATION_CANCELLED");
    assertThat(event.routingKey()).isEqualTo("booking.reservation.cancelled.v1");
    assertThat(event.payload()).contains("\"status\":\"CANCELLED\"");
  }

  @Test
  void createsVersionedCompletedEnvelope() {
    OutboxEvent event =
        new OutboxEventFactory(new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC))
            .create(booking(BookingStatus.COMPLETED));

    assertThat(event.eventType()).isEqualTo("BOOKING_RESERVATION_COMPLETED");
    assertThat(event.routingKey()).isEqualTo("booking.reservation.completed.v1");
    assertThat(event.payload()).contains("\"status\":\"COMPLETED\"");
  }

  private static BookingReservation booking(BookingStatus status) {
    return new BookingReservation(
        99L,
        "B-1",
        "R-1",
        1L,
        2L,
        1,
        status,
        "allocate:1",
        "release:1",
        0L,
        null,
        null,
        null,
        null);
  }
}
