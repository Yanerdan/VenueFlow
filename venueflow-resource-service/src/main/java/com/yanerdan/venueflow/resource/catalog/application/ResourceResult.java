package com.yanerdan.venueflow.resource.catalog.application;

import com.yanerdan.venueflow.resource.catalog.domain.ApprovalMode;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceEntity;
import java.time.LocalDateTime;
import java.util.List;

public record ResourceResult(
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

  public static ResourceResult from(ResourceEntity entity) {
    return new ResourceResult(
        entity.getId(),
        entity.getResourceNo(),
        entity.getCategoryId(),
        entity.getName(),
        entity.getDescription(),
        entity.getLocation(),
        entity.getCapacity(),
        entity.getOwnerDepartment(),
        entity.getApproverExternalUserId(),
        entity.getApprovalMode(),
        entity.getFinalApproverExternalUserId(),
        entity.getBookingNotice(),
        entity.getMinAdvanceHours(),
        entity.getMaxAdvanceDays(),
        entity.getMaxDurationMinutes(),
        List.of(),
        entity.getStatus(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public ResourceResult(
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
      ResourceStatus status,
      Long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this(
        id,
        resourceNo,
        categoryId,
        name,
        description,
        location,
        capacity,
        ownerDepartment,
        approverExternalUserId,
        approvalMode,
        finalApproverExternalUserId,
        bookingNotice,
        minAdvanceHours,
        maxAdvanceDays,
        maxDurationMinutes,
        List.of(),
        status,
        version,
        createdAt,
        updatedAt);
  }

  public ResourceResult(
      Long id,
      String resourceNo,
      Long categoryId,
      String name,
      String description,
      String location,
      Integer capacity,
      ResourceStatus status,
      Long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this(
        id,
        resourceNo,
        categoryId,
        name,
        description,
        location,
        capacity,
        null,
        null,
        ApprovalMode.DIRECT,
        null,
        null,
        0,
        90,
        480,
        List.of(),
        status,
        version,
        createdAt,
        updatedAt);
  }

  public ResourceResult {
    approvalStages = approvalStages == null ? List.of() : List.copyOf(approvalStages);
  }

  public ResourceResult withApprovalStages(List<ApprovalStageResult> stages) {
    return new ResourceResult(
        id,
        resourceNo,
        categoryId,
        name,
        description,
        location,
        capacity,
        ownerDepartment,
        approverExternalUserId,
        approvalMode,
        finalApproverExternalUserId,
        bookingNotice,
        minAdvanceHours,
        maxAdvanceDays,
        maxDurationMinutes,
        stages,
        status,
        version,
        createdAt,
        updatedAt);
  }
}
