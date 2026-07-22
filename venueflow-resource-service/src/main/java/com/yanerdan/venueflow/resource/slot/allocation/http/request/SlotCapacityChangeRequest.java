package com.yanerdan.venueflow.resource.slot.allocation.http.request;

import com.yanerdan.venueflow.resource.slot.allocation.application.SlotCapacityChangeCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SlotCapacityChangeRequest(
    @NotBlank(message = "operationId must not be blank")
        @Size(max = 64, message = "operationId must not exceed 64 characters")
        String operationId,
    @Positive(message = "quantity must be positive") int quantity) {

  public SlotCapacityChangeCommand toCommand(Long slotId) {
    return new SlotCapacityChangeCommand(slotId, operationId, quantity);
  }
}
