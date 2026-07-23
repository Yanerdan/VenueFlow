package com.yanerdan.venueflow.booking.outbox.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence")
public class OutboxEventFactory {
  static final int MAX_PAYLOAD_BYTES = 4096;
  static final int MAX_HEADERS_BYTES = 1024;
  private static final String PRODUCER = "venueflow-booking-service";
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public OutboxEventFactory() {
    this(new ObjectMapper(), Clock.systemUTC());
  }

  OutboxEventFactory(ObjectMapper objectMapper, Clock clock) {
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public OutboxEvent create(BookingReservation booking) {
    boolean confirmed = booking.status() == BookingStatus.CONFIRMED;
    String type = confirmed ? "BOOKING_RESERVATION_CONFIRMED" : "BOOKING_RESERVATION_CANCELLED";
    String externalType =
        confirmed ? "booking.reservation.confirmed" : "booking.reservation.cancelled";
    String eventId = UUID.randomUUID().toString();
    Instant occurredAt = clock.instant();
    String payload =
        json(
            new Envelope(
                eventId,
                externalType,
                1,
                occurredAt.toString(),
                PRODUCER,
                "BOOKING",
                booking.bookingNo(),
                null,
                new Payload(
                    booking.bookingNo(),
                    booking.userId(),
                    booking.slotId(),
                    booking.quantity(),
                    booking.status().name())));
    String headers = json(new Headers("application/json", "UTF-8"));
    requireSize(payload, MAX_PAYLOAD_BYTES, "payload");
    requireSize(headers, MAX_HEADERS_BYTES, "headers");
    LocalDateTime createdAt = LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC);
    return new OutboxEvent(
        null,
        eventId,
        "BOOKING",
        booking.bookingNo(),
        type,
        1,
        externalType + ".v1",
        payload,
        headers,
        OutboxStatus.NEW,
        0,
        null,
        null,
        null,
        createdAt,
        null,
        null,
        0L);
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Outbox event serialization failed", exception);
    }
  }

  private static void requireSize(String value, int maximum, String field) {
    if (value.getBytes(StandardCharsets.UTF_8).length > maximum) {
      throw new IllegalArgumentException("Outbox " + field + " exceeds limit");
    }
  }

  private record Envelope(
      String eventId,
      String eventType,
      int eventVersion,
      String occurredAt,
      String producer,
      String aggregateType,
      String aggregateId,
      String traceId,
      Payload payload) {}

  private record Payload(String bookingNo, long userId, long slotId, int quantity, String status) {}

  private record Headers(String contentType, String contentEncoding) {}
}
