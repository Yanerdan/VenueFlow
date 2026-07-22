package com.yanerdan.venueflow.resource.slot.allocation.application;

import com.yanerdan.venueflow.resource.slot.allocation.domain.SlotAllocationOperationType;
import com.yanerdan.venueflow.resource.slot.allocation.persistence.entity.ResourceSlotAllocationEntity;
import java.time.LocalDateTime;

public record SlotAllocationOperationResult(
    Long id,
    String operationId,
    SlotAllocationOperationType operationType,
    int quantity,
    int occupiedQuantityAfter,
    LocalDateTime createdAt) {

  static SlotAllocationOperationResult from(ResourceSlotAllocationEntity entity) {
    return new SlotAllocationOperationResult(
        entity.getId(),
        entity.getOperationId(),
        entity.getOperationType(),
        entity.getQuantity(),
        entity.getOccupiedQuantityAfter(),
        entity.getCreatedAt());
  }
}
