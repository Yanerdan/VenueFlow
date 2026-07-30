package com.yanerdan.venueflow.resource.catalog.http.response;

import com.yanerdan.venueflow.resource.catalog.application.ApprovalStageResult;
import com.yanerdan.venueflow.resource.catalog.application.ResourceResult;
import com.yanerdan.venueflow.resource.catalog.domain.ApprovalMode;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ResourceResponse(
    Long id,
    String resourceNo,
    Long categoryId,
    String name,
    String description,
    String location,
    Integer capacity,
    String ownerDepartment,
    String approverExternalUserId,
    ApprovalMode approvalMode,
    String finalApproverExternalUserId,
    String bookingNotice,
    Integer minAdvanceHours,
    Integer maxAdvanceDays,
    Integer maxDurationMinutes,
    List<ApprovalStageResult> approvalStages,
    ResourceStatus status,
    Long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static ResourceResponse from(ResourceResult result) {
    return new ResourceResponse(
        result.id(),
        result.resourceNo(),
        result.categoryId(),
        result.name(),
        result.description(),
        result.location(),
        result.capacity(),
        result.ownerDepartment(),
        result.approverExternalUserId(),
        result.approvalMode(),
        result.finalApproverExternalUserId(),
        result.bookingNotice(),
        result.minAdvanceHours(),
        result.maxAdvanceDays(),
        result.maxDurationMinutes(),
        result.approvalStages(),
        result.status(),
        result.version(),
        result.createdAt(),
        result.updatedAt());
  }
}
