package com.yanerdan.venueflow.resource.slot.allocation.http.response;

import com.yanerdan.venueflow.resource.slot.allocation.application.SlotAllocationOperationResult;
import com.yanerdan.venueflow.resource.slot.allocation.domain.SlotAllocationOperationType;
import java.time.LocalDateTime;

public record SlotAllocationOperationResponse(
    Long id,
    String operationId,
    SlotAllocationOperationType operationType,
    int quantity,
    int occupiedQuantityAfter,
    LocalDateTime createdAt) {

  public static SlotAllocationOperationResponse from(SlotAllocationOperationResult result) {
    return new SlotAllocationOperationResponse(
        result.id(),
        result.operationId(),
        result.operationType(),
        result.quantity(),
        result.occupiedQuantityAfter(),
        result.createdAt());
  }
}
