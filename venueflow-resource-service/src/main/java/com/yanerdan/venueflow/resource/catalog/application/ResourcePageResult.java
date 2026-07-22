package com.yanerdan.venueflow.resource.catalog.application;

import java.util.List;

public record ResourcePageResult(
    List<ResourceResult> items, int page, int size, long totalElements, long totalPages) {

  public ResourcePageResult {
    items = List.copyOf(items);

    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }

    if (size < 1) {
      throw new IllegalArgumentException("size must be positive");
    }

    if (totalElements < 0) {
      throw new IllegalArgumentException("totalElements must not be negative");
    }

    if (totalPages < 0) {
      throw new IllegalArgumentException("totalPages must not be negative");
    }
  }

  public static ResourcePageResult of(
      List<ResourceResult> items, ResourcePageQuery query, long totalElements) {
    long totalPages = totalElements == 0 ? 0 : ((totalElements - 1) / query.size()) + 1;

    return new ResourcePageResult(items, query.page(), query.size(), totalElements, totalPages);
  }
}
