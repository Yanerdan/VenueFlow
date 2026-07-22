package com.yanerdan.venueflow.resource.catalog.application;

import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import java.util.Objects;

public record ChangeResourceStatusCommand(
    Long resourceId, ResourceStatus targetStatus, Long expectedVersion) {

  public ChangeResourceStatusCommand {
    if (resourceId == null || resourceId <= 0) {
      throw new IllegalArgumentException("resourceId must be positive");
    }

    targetStatus = Objects.requireNonNull(targetStatus, "targetStatus must not be null");

    if (expectedVersion == null || expectedVersion <= 0) {
      throw new IllegalArgumentException("expectedVersion must be positive");
    }
  }
}
