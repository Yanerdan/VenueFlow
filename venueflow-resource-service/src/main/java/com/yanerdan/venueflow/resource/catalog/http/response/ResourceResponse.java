package com.yanerdan.venueflow.resource.catalog.http.response;

import com.yanerdan.venueflow.resource.catalog.application.ResourceResult;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import java.time.LocalDateTime;

public record ResourceResponse(
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

  public static ResourceResponse from(ResourceResult result) {
    return new ResourceResponse(
        result.id(),
        result.resourceNo(),
        result.categoryId(),
        result.name(),
        result.description(),
        result.location(),
        result.capacity(),
        result.status(),
        result.version(),
        result.createdAt(),
        result.updatedAt());
  }
}
