package com.yanerdan.venueflow.resource.catalog.application;

import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import com.yanerdan.venueflow.resource.catalog.domain.ApprovalMode;
import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceEntity;
import java.time.LocalDateTime;

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
        entity.getStatus(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public ResourceResult(
      Long id, String resourceNo, Long categoryId, String name, String description, String location,
      Integer capacity, ResourceStatus status, Long version, LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this(id, resourceNo, categoryId, name, description, location, capacity, null, null,
        ApprovalMode.DIRECT, null, status, version, createdAt, updatedAt);
  }
}
