package com.yanerdan.venueflow.resource.catalog.application;

import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
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
        entity.getStatus(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
