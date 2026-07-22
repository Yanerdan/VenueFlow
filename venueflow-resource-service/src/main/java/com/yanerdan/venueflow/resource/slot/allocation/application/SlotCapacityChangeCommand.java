package com.yanerdan.venueflow.resource.slot.allocation.application;

import java.util.Objects;

public record SlotCapacityChangeCommand(Long slotId, String operationId, int quantity) {

  public SlotCapacityChangeCommand {
    if (slotId == null || slotId <= 0) {
      throw new IllegalArgumentException("slotId must be positive");
    }
    Objects.requireNonNull(operationId, "operationId must not be null");
    if (operationId.isBlank() || operationId.length() > 64) {
      throw new IllegalArgumentException("operationId must contain at most 64 characters");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
  }
}
