package com.yanerdan.venueflow.resource.slot.allocation.application;

import com.yanerdan.venueflow.resource.slot.allocation.domain.SlotAllocationOperationType;

public record SlotCapacityChangeResult(
    String operationId,
    SlotAllocationOperationType operationType,
    int quantity,
    SlotCapacityResult capacity) {}
