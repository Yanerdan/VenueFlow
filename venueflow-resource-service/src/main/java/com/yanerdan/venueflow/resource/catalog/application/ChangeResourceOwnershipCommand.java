package com.yanerdan.venueflow.resource.catalog.application;

public record ChangeResourceOwnershipCommand(
    Long resourceId, String ownerDepartment, String approverExternalUserId, Long expectedVersion) {

  public ChangeResourceOwnershipCommand {
    if (resourceId == null || resourceId <= 0) {
      throw new IllegalArgumentException("resourceId must be positive");
    }
    ownerDepartment = normalize(ownerDepartment);
    if (ownerDepartment != null && ownerDepartment.length() > 160) {
      throw new IllegalArgumentException("ownerDepartment must not exceed 160 characters");
    }
    approverExternalUserId = normalize(approverExternalUserId);
    if (approverExternalUserId != null && approverExternalUserId.length() > 64) {
      throw new IllegalArgumentException("approverExternalUserId must not exceed 64 characters");
    }
    if (expectedVersion == null || expectedVersion <= 0) {
      throw new IllegalArgumentException("expectedVersion must be positive");
    }
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
