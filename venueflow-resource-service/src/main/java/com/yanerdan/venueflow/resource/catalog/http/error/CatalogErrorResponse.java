package com.yanerdan.venueflow.resource.catalog.http.error;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record CatalogErrorResponse(
    String code, String message, Map<String, Object> details, String traceId, Instant timestamp) {

  public CatalogErrorResponse {
    code = Objects.requireNonNull(code, "code must not be null");
    message = Objects.requireNonNull(message, "message must not be null");
    details = Map.copyOf(Objects.requireNonNull(details, "details must not be null"));
    traceId = Objects.requireNonNull(traceId, "traceId must not be null");
    timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
  }
}
