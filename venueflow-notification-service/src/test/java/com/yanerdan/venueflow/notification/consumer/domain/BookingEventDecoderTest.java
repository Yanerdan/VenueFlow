package com.yanerdan.venueflow.notification.consumer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class BookingEventDecoderTest {
  private final BookingEventDecoder decoder = new BookingEventDecoder(new ObjectMapper(), 4096);

  @Test
  void decodesStableConfirmationAndDerivesNotification() {
    BookingEvent event =
        decoder.decode(
            confirmation("c88d6348-92c5-4ead-b14a-22557b60610d").getBytes(StandardCharsets.UTF_8),
            BookingEventDecoder.CONFIRMED_ROUTE,
            "application/json",
            "UTF-8");

    assertThat(event.status()).isEqualTo("CONFIRMED");
    assertThat(event.payloadHash()).hasSize(64);
    assertThat(NotificationDraft.from(event).type()).isEqualTo("BOOKING_CONFIRMED");
  }

  @Test
  void canonicalHashIgnoresJsonPropertyOrder() {
    String eventId = "c88d6348-92c5-4ead-b14a-22557b60610d";
    BookingEvent first =
        decoder.decode(
            confirmation(eventId).getBytes(StandardCharsets.UTF_8),
            BookingEventDecoder.CONFIRMED_ROUTE,
            "application/json",
            "UTF-8");
    String reordered =
        """
        {"payload":{"status":"CONFIRMED","quantity":1,"slotId":2,"userId":1,"bookingNo":"B-1"},
        "aggregateId":"B-1","aggregateType":"BOOKING","producer":"venueflow-booking-service",
        "occurredAt":"2026-07-23T10:00:00Z","eventVersion":1,
        "eventType":"booking.reservation.confirmed","eventId":"%s","traceId":null}
        """
            .formatted(eventId);
    BookingEvent second =
        decoder.decode(
            reordered.getBytes(StandardCharsets.UTF_8),
            BookingEventDecoder.CONFIRMED_ROUTE,
            "application/json",
            "UTF-8");

    assertThat(second.payloadHash()).isEqualTo(first.payloadHash());
  }

  @Test
  void rejectsRouteAndPayloadMismatch() {
    assertThatThrownBy(
            () ->
                decoder.decode(
                    confirmation("c88d6348-92c5-4ead-b14a-22557b60610d")
                        .getBytes(StandardCharsets.UTF_8),
                    BookingEventDecoder.CANCELLED_ROUTE,
                    "application/json",
                    "UTF-8"))
        .isInstanceOf(EnvelopeException.class)
        .extracting("failureCode")
        .isEqualTo(FailureCode.UNSUPPORTED_EVENT);
  }

  @Test
  void rejectsOversizedAndWrongContentType() {
    assertThatThrownBy(
            () ->
                decoder.decode(
                    new byte[4097],
                    BookingEventDecoder.CONFIRMED_ROUTE,
                    "application/json",
                    "UTF-8"))
        .isInstanceOf(EnvelopeException.class);
    assertThatThrownBy(
            () ->
                decoder.decode(
                    confirmation("c88d6348-92c5-4ead-b14a-22557b60610d")
                        .getBytes(StandardCharsets.UTF_8),
                    BookingEventDecoder.CONFIRMED_ROUTE,
                    "text/plain",
                    "UTF-8"))
        .isInstanceOf(EnvelopeException.class);
  }

  @Test
  void decodesExpirationAndDerivesTypedNotification() {
    BookingEvent event =
        decoder.decode(
            expiration("1ad86725-dc3b-4565-b669-4e88e8fb6961").getBytes(StandardCharsets.UTF_8),
            BookingEventDecoder.EXPIRED_ROUTE,
            "application/json",
            "UTF-8");

    assertThat(event.status()).isEqualTo("EXPIRED");
    assertThat(NotificationDraft.from(event).type()).isEqualTo("BOOKING_EXPIRED");
  }

  public static String confirmation(String eventId) {
    return """
        {
          "eventId":"%s",
          "eventType":"booking.reservation.confirmed",
          "eventVersion":1,
          "occurredAt":"2026-07-23T10:00:00Z",
          "producer":"venueflow-booking-service",
          "aggregateType":"BOOKING",
          "aggregateId":"B-1",
          "traceId":null,
          "payload":{"bookingNo":"B-1","userId":1,"slotId":2,"quantity":1,"status":"CONFIRMED"}
        }
        """
        .formatted(eventId);
  }

  public static String expiration(String eventId) {
    return """
        {
          "eventId":"%s",
          "eventType":"booking.reservation.expired",
          "eventVersion":1,
          "occurredAt":"2026-07-26T10:00:00Z",
          "producer":"venueflow-booking-service",
          "aggregateType":"BOOKING",
          "aggregateId":"B-1",
          "traceId":null,
          "payload":{"bookingNo":"B-1","userId":1,"slotId":2,"quantity":1,"status":"EXPIRED"}
        }
        """
        .formatted(eventId);
  }
}
