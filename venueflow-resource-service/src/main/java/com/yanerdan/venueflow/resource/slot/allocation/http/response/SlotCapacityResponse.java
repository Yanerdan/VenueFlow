package com.yanerdan.venueflow.resource.slot.allocation.http.response;

import com.yanerdan.venueflow.resource.slot.allocation.application.SlotCapacityResult;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;

public record SlotCapacityResponse(
    Long slotId,
    int staticCapacity,
    int occupiedQuantity,
    int availableQuantity,
    ResourceSlotStatus slotStatus) {

  public static SlotCapacityResponse from(SlotCapacityResult result) {
    return new SlotCapacityResponse(
        result.slotId(),
        result.staticCapacity(),
        result.occupiedQuantity(),
        result.availableQuantity(),
        result.slotStatus());
  }
}
