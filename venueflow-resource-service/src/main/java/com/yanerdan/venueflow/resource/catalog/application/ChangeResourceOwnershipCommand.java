package com.yanerdan.venueflow.resource.catalog.application;

import com.yanerdan.venueflow.resource.catalog.domain.ApprovalMode;

public record ChangeResourceOwnershipCommand(
    Long resourceId,
    String ownerDepartment,
    String approverExternalUserId,
    ApprovalMode approvalMode,
    String finalApproverExternalUserId,
    Long expectedVersion) {

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
    approvalMode = approvalMode == null ? ApprovalMode.DIRECT : approvalMode;
    finalApproverExternalUserId = normalize(finalApproverExternalUserId);
    if (finalApproverExternalUserId != null && finalApproverExternalUserId.length() > 64) {
      throw new IllegalArgumentException(
          "finalApproverExternalUserId must not exceed 64 characters");
    }
    if (approvalMode == ApprovalMode.TWO_STAGE) {
      if (approverExternalUserId == null || finalApproverExternalUserId == null) {
        throw new IllegalArgumentException("Two-stage approval requires both approvers");
      }
      if (approverExternalUserId.equals(finalApproverExternalUserId)) {
        throw new IllegalArgumentException("Approval stages require distinct approvers");
      }
    } else {
      finalApproverExternalUserId = null;
    }
    if (expectedVersion == null || expectedVersion <= 0) {
      throw new IllegalArgumentException("expectedVersion must be positive");
    }
  }

  public ChangeResourceOwnershipCommand(
      Long resourceId,
      String ownerDepartment,
      String approverExternalUserId,
      Long expectedVersion) {
    this(
        resourceId,
        ownerDepartment,
        approverExternalUserId,
        ApprovalMode.DIRECT,
        null,
        expectedVersion);
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
