package com.yanerdan.venueflow.resource.slot.application;

import java.time.Instant;
import java.util.Objects;

public record CreateResourceSlotCommand(Long resourceId, Instant startAt, Instant endAt) {

  public CreateResourceSlotCommand {
    if (resourceId == null || resourceId <= 0) {
      throw new IllegalArgumentException("resourceId must be positive");
    }
    Objects.requireNonNull(startAt, "startAt must not be null");
    Objects.requireNonNull(endAt, "endAt must not be null");
  }
}
