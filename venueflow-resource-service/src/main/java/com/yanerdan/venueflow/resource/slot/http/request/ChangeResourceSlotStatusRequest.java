package com.yanerdan.venueflow.resource.slot.http.request;

import com.yanerdan.venueflow.resource.slot.application.ChangeResourceSlotStatusCommand;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChangeResourceSlotStatusRequest(
    @NotNull(message = "targetStatus must not be null") ResourceSlotStatus targetStatus,
    @NotNull(message = "expectedVersion must not be null")
        @Positive(message = "expectedVersion must be positive")
        Long expectedVersion) {

  public ChangeResourceSlotStatusCommand toCommand(Long slotId) {
    return new ChangeResourceSlotStatusCommand(slotId, targetStatus, expectedVersion);
  }
}
