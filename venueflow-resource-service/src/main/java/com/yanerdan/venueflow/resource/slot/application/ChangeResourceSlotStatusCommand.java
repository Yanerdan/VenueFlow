package com.yanerdan.venueflow.resource.slot.application;

import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;
import java.util.Objects;

public record ChangeResourceSlotStatusCommand(
    Long slotId, ResourceSlotStatus targetStatus, Long expectedVersion) {

  public ChangeResourceSlotStatusCommand {
    if (slotId == null || slotId <= 0) {
      throw new IllegalArgumentException("slotId must be positive");
    }
    Objects.requireNonNull(targetStatus, "targetStatus must not be null");
    if (expectedVersion == null || expectedVersion <= 0) {
      throw new IllegalArgumentException("expectedVersion must be positive");
    }
  }
}
