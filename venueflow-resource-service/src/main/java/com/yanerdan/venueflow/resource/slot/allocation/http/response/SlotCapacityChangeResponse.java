package com.yanerdan.venueflow.resource.slot.allocation.http.response;

import com.yanerdan.venueflow.resource.slot.allocation.application.SlotCapacityChangeResult;
import com.yanerdan.venueflow.resource.slot.allocation.domain.SlotAllocationOperationType;

public record SlotCapacityChangeResponse(
    String operationId,
    SlotAllocationOperationType operationType,
    int quantity,
    SlotCapacityResponse capacity) {

  public static SlotCapacityChangeResponse from(SlotCapacityChangeResult result) {
    return new SlotCapacityChangeResponse(
        result.operationId(),
        result.operationType(),
        result.quantity(),
        SlotCapacityResponse.from(result.capacity()));
  }
}
