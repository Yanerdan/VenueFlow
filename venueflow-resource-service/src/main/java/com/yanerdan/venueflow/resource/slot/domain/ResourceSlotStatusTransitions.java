package com.yanerdan.venueflow.resource.slot.domain;

import java.util.Objects;

public final class ResourceSlotStatusTransitions {

  private ResourceSlotStatusTransitions() {}

  public static boolean canTransition(
      ResourceSlotStatus currentStatus, ResourceSlotStatus targetStatus) {
    Objects.requireNonNull(currentStatus, "currentStatus must not be null");
    Objects.requireNonNull(targetStatus, "targetStatus must not be null");
    return currentStatus != targetStatus;
  }
}
