package com.yanerdan.venueflow.resource.catalog.http.request;

import com.yanerdan.venueflow.resource.catalog.application.ResourcePageQuery;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record ResourcePageRequest(
    @Min(value = 0, message = "page must not be negative") Integer page,
    @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must not exceed 100")
        Integer size,
    @Positive(message = "categoryId must be positive") Long categoryId,
    ResourceStatus status) {

  public ResourcePageQuery toQuery() {
    return ResourcePageQuery.of(page, size, categoryId, status);
  }
}
