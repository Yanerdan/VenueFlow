package com.yanerdan.venueflow.resource.slot.application;

import com.yanerdan.venueflow.resource.catalog.domain.ApprovalMode;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;
import com.yanerdan.venueflow.resource.slot.persistence.entity.ResourceSlotEntity;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record ResourceSlotResult(
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

  public static ResourceSlotResult from(ResourceSlotEntity entity) {
    return new ResourceSlotResult(
        entity.getId(),
        entity.getResourceId(),
        null,
        null,
        ApprovalMode.DIRECT,
        null,
        null,
        0,
        90,
        480,
        entity.getStartAt().toInstant(ZoneOffset.UTC),
        entity.getEndAt().toInstant(ZoneOffset.UTC),
        entity.getStatus(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public ResourceSlotResult(
      Long id,
      Long resourceId,
      Instant startAt,
      Instant endAt,
      ResourceSlotStatus status,
      Long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this(
        id,
        resourceId,
        null,
        null,
        ApprovalMode.DIRECT,
        null,
        null,
        0,
        90,
        480,
        startAt,
        endAt,
        status,
        version,
        createdAt,
        updatedAt);
  }

  public ResourceSlotResult withResourcePolicy(
      String department,
      String approverId,
      ApprovalMode mode,
      String finalApproverId,
      String notice,
      Integer minimumHours,
      Integer maximumDays,
      Integer maximumMinutes) {
    return new ResourceSlotResult(
        id,
        resourceId,
        department,
        approverId,
        mode,
        finalApproverId,
        notice,
        minimumHours,
        maximumDays,
        maximumMinutes,
        startAt,
        endAt,
        status,
        version,
        createdAt,
        updatedAt);
  }
}
