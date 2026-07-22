package com.yanerdan.venueflow.resource.slot.http.request;

import com.yanerdan.venueflow.resource.slot.application.CreateResourceSlotCommand;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateResourceSlotRequest(
    @NotNull(message = "startAt must not be null") OffsetDateTime startAt,
    @NotNull(message = "endAt must not be null") OffsetDateTime endAt) {

  public CreateResourceSlotCommand toCommand(Long resourceId) {
    return new CreateResourceSlotCommand(resourceId, startAt.toInstant(), endAt.toInstant());
  }
}
