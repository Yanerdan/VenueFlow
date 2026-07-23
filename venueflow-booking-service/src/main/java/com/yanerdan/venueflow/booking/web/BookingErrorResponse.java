package com.yanerdan.venueflow.booking.web;

import java.time.Instant;
import java.util.Map;

public record BookingErrorResponse(
    String code, String message, Map<String, Object> details, String traceId, Instant timestamp) {
  public BookingErrorResponse {
    details = Map.copyOf(details);
  }
}
