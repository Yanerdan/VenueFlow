package com.yanerdan.venueflow.user.profile.web.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record UserProfileErrorResponse(
    UserProfileErrorCode code,
    String message,
    Map<String, Object> details,
    String traceId,
    Instant timestamp) {

  public UserProfileErrorResponse {
    Objects.requireNonNull(code, "Error code must not be null");

    Objects.requireNonNull(message, "Error message must not be null");

    Objects.requireNonNull(details, "Error details must not be null");

    Objects.requireNonNull(traceId, "Trace id must not be null");

    Objects.requireNonNull(timestamp, "Timestamp must not be null");

    details = Map.copyOf(new LinkedHashMap<>(details));
  }
}
