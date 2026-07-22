package com.yanerdan.venueflow.resource.catalog.http.request;

import com.yanerdan.venueflow.resource.catalog.application.ChangeResourceStatusCommand;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChangeResourceStatusRequest(
    @NotNull(message = "targetStatus must not be null") ResourceStatus targetStatus,
    @NotNull(message = "expectedVersion must not be null")
        @Positive(message = "expectedVersion must be positive")
        Long expectedVersion) {

  public ChangeResourceStatusCommand toCommand(Long resourceId) {
    return new ChangeResourceStatusCommand(resourceId, targetStatus, expectedVersion);
  }
}
