package com.yanerdan.venueflow.notification.consumer.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

public final class BookingEventDecoder {
  public static final String CONFIRMED_ROUTE = "booking.reservation.confirmed.v1";
  public static final String CANCELLED_ROUTE = "booking.reservation.cancelled.v1";
  private static final String CONFIRMED_TYPE = "booking.reservation.confirmed";
  private static final String CANCELLED_TYPE = "booking.reservation.cancelled";
  private static final Set<String> ROUTES = Set.of(CONFIRMED_ROUTE, CANCELLED_ROUTE);
  private static final String PRODUCER = "venueflow-booking-service";
  private final ObjectMapper objectMapper;
  private final int maximumBytes;

  public BookingEventDecoder(ObjectMapper objectMapper, int maximumBytes) {
    if (maximumBytes < 512 || maximumBytes > 65_536) {
      throw new IllegalArgumentException("maximumBytes is outside the allowed range");
    }
    this.objectMapper = objectMapper;
    this.maximumBytes = maximumBytes;
  }

  public BookingEvent decode(
      byte[] body, String routingKey, String contentType, String contentEncoding) {
    if (body == null || body.length == 0 || body.length > maximumBytes) {
      throw new EnvelopeException(FailureCode.MESSAGE_TOO_LARGE);
    }
    if (!"application/json".equalsIgnoreCase(contentType)
        || (contentEncoding != null && !"UTF-8".equalsIgnoreCase(contentEncoding))) {
      throw new EnvelopeException(FailureCode.INVALID_CONTENT_TYPE);
    }
    if (!ROUTES.contains(routingKey)) {
      throw new EnvelopeException(FailureCode.UNSUPPORTED_EVENT);
    }

    try {
      JsonNode root = objectMapper.readTree(body);
      JsonNode payload = requiredObject(root, "payload");
      String eventId = boundedText(root, "eventId", 36);
      UUID.fromString(eventId);
      String eventType = boundedText(root, "eventType", 64);
      int eventVersion = positiveInt(root, "eventVersion");
      Instant occurredAt = Instant.parse(boundedText(root, "occurredAt", 40));
      String producer = boundedText(root, "producer", 64);
      String aggregateType = boundedText(root, "aggregateType", 32);
      String aggregateId = boundedText(root, "aggregateId", 64);
      String traceId = optionalText(root, "traceId", 128);
      String bookingNo = boundedText(payload, "bookingNo", 64);
      long userId = positiveLong(payload, "userId");
      long slotId = positiveLong(payload, "slotId");
      int quantity = positiveInt(payload, "quantity");
      String status = boundedText(payload, "status", 16);

      validateContract(
          routingKey,
          eventType,
          eventVersion,
          producer,
          aggregateType,
          aggregateId,
          bookingNo,
          status);

      CanonicalEnvelope canonical =
          new CanonicalEnvelope(
              eventId,
              eventType,
              eventVersion,
              occurredAt.toString(),
              producer,
              aggregateType,
              aggregateId,
              traceId,
              bookingNo,
              userId,
              slotId,
              quantity,
              status,
              routingKey);
      String hash = sha256(objectMapper.writeValueAsBytes(canonical));
      return new BookingEvent(
          eventId,
          eventType,
          eventVersion,
          occurredAt,
          producer,
          aggregateType,
          aggregateId,
          traceId,
          bookingNo,
          userId,
          slotId,
          quantity,
          status,
          routingKey,
          hash);
    } catch (EnvelopeException exception) {
      throw exception;
    } catch (IOException | IllegalArgumentException | DateTimeException exception) {
      throw new EnvelopeException(FailureCode.MALFORMED_ENVELOPE);
    }
  }

  public static String rawFingerprint(byte[] body) {
    return sha256(body == null ? new byte[0] : body);
  }

  private static void validateContract(
      String routingKey,
      String eventType,
      int eventVersion,
      String producer,
      String aggregateType,
      String aggregateId,
      String bookingNo,
      String status) {
    boolean confirmed = CONFIRMED_ROUTE.equals(routingKey);
    String expectedType = confirmed ? CONFIRMED_TYPE : CANCELLED_TYPE;
    String expectedStatus = confirmed ? "CONFIRMED" : "CANCELLED";
    if (eventVersion != 1
        || !expectedType.equals(eventType)
        || !expectedStatus.equals(status)
        || !PRODUCER.equals(producer)
        || !"BOOKING".equals(aggregateType)
        || !bookingNo.equals(aggregateId)) {
      throw new EnvelopeException(FailureCode.UNSUPPORTED_EVENT);
    }
  }

  private static JsonNode requiredObject(JsonNode parent, String field) {
    JsonNode value = parent == null ? null : parent.get(field);
    if (value == null || !value.isObject()) {
      throw new EnvelopeException(FailureCode.MALFORMED_ENVELOPE);
    }
    return value;
  }

  private static String boundedText(JsonNode parent, String field, int maximum) {
    JsonNode value = parent == null ? null : parent.get(field);
    if (value == null || !value.isTextual()) {
      throw new EnvelopeException(FailureCode.MALFORMED_ENVELOPE);
    }
    String text = value.textValue();
    if (text.isBlank() || text.length() > maximum) {
      throw new EnvelopeException(FailureCode.MALFORMED_ENVELOPE);
    }
    return text;
  }

  private static String optionalText(JsonNode parent, String field, int maximum) {
    JsonNode value = parent == null ? null : parent.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    if (!value.isTextual() || value.textValue().length() > maximum) {
      throw new EnvelopeException(FailureCode.MALFORMED_ENVELOPE);
    }
    return value.textValue();
  }

  private static int positiveInt(JsonNode parent, String field) {
    JsonNode value = parent == null ? null : parent.get(field);
    if (value == null || !value.canConvertToInt() || value.intValue() <= 0) {
      throw new EnvelopeException(FailureCode.MALFORMED_ENVELOPE);
    }
    return value.intValue();
  }

  private static long positiveLong(JsonNode parent, String field) {
    JsonNode value = parent == null ? null : parent.get(field);
    if (value == null || !value.canConvertToLong() || value.longValue() <= 0) {
      throw new EnvelopeException(FailureCode.MALFORMED_ENVELOPE);
    }
    return value.longValue();
  }

  private static String sha256(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private record CanonicalEnvelope(
      String eventId,
      String eventType,
      int eventVersion,
      String occurredAt,
      String producer,
      String aggregateType,
      String aggregateId,
      String traceId,
      String bookingNo,
      long userId,
      long slotId,
      int quantity,
      String status,
      String routingKey) {}
}
