package com.yanerdan.venueflow.resource.catalog.application;

import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceCategoryEntity;
import java.time.LocalDateTime;

public record CategoryResult(
    Long id, String code, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {

  public static CategoryResult from(ResourceCategoryEntity entity) {
    return new CategoryResult(
        entity.getId(),
        entity.getCode(),
        entity.getName(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
