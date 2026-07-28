package com.yanerdan.venueflow.resource.catalog.http.request;

import com.yanerdan.venueflow.resource.catalog.application.ChangeResourceOwnershipCommand;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChangeResourceOwnershipRequest(
    @Size(max = 160) String ownerDepartment,
    @Size(max = 64) String approverExternalUserId,
    @Positive Long expectedVersion) {

  public ChangeResourceOwnershipCommand toCommand(Long resourceId) {
    return new ChangeResourceOwnershipCommand(
        resourceId, ownerDepartment, approverExternalUserId, expectedVersion);
  }
}
