package com.yanerdan.venueflow.resource.catalog.http.response;

import com.yanerdan.venueflow.resource.catalog.application.CategoryResult;
import java.time.LocalDateTime;

public record CategoryResponse(
    Long id, String code, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {

  public static CategoryResponse from(CategoryResult result) {
    return new CategoryResponse(
        result.id(), result.code(), result.name(), result.createdAt(), result.updatedAt());
  }
}
