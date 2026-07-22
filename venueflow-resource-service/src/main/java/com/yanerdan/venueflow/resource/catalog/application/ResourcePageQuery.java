package com.yanerdan.venueflow.resource.catalog.application;

import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;

public record ResourcePageQuery(int page, int size, Long categoryId, ResourceStatus status) {

  public static final int DEFAULT_PAGE = 0;
  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  public ResourcePageQuery {
    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }

    if (size < 1 || size > MAX_SIZE) {
      throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
    }

    if (categoryId != null && categoryId <= 0) {
      throw new IllegalArgumentException("categoryId must be positive");
    }
  }

  public static ResourcePageQuery of(
      Integer page, Integer size, Long categoryId, ResourceStatus status) {
    return new ResourcePageQuery(
        page == null ? DEFAULT_PAGE : page, size == null ? DEFAULT_SIZE : size, categoryId, status);
  }

  public long offset() {
    return (long) page * size;
  }
}
