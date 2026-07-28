package com.yanerdan.venueflow.resource.slot.http.response;

import com.yanerdan.venueflow.resource.catalog.domain.ApprovalMode;
import com.yanerdan.venueflow.resource.slot.application.ResourceSlotResult;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;
import java.time.Instant;
import java.time.LocalDateTime;

public record ResourceSlotResponse(
    Long id,
    Long resourceId,
    String ownerDepartment,
    String approverExternalUserId,
    ApprovalMode approvalMode,
    String finalApproverExternalUserId,
    String bookingNotice,
    Integer minAdvanceHours,
    Integer maxAdvanceDays,
    Integer maxDurationMinutes,
    Instant startAt,
    Instant endAt,
    ResourceSlotStatus status,
    Long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static ResourceSlotResponse from(ResourceSlotResult result) {
    return new ResourceSlotResponse(
        result.id(),
        result.resourceId(),
        result.ownerDepartment(),
        result.approverExternalUserId(),
        result.approvalMode(),
        result.finalApproverExternalUserId(),
        result.bookingNotice(),
        result.minAdvanceHours(),
        result.maxAdvanceDays(),
        result.maxDurationMinutes(),
        result.startAt(),
        result.endAt(),
        result.status(),
        result.version(),
        result.createdAt(),
        result.updatedAt());
  }
}
