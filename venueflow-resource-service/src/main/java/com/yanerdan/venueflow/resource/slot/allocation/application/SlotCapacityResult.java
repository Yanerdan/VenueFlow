package com.yanerdan.venueflow.resource.slot.allocation.application;

import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;

public record SlotCapacityResult(
    Long slotId,
    int staticCapacity,
    int occupiedQuantity,
    int availableQuantity,
    ResourceSlotStatus slotStatus) {}
