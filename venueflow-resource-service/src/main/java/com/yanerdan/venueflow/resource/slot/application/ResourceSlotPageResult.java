package com.yanerdan.venueflow.resource.slot.application;

import java.util.List;

public record ResourceSlotPageResult(
    List<ResourceSlotResult> items, int page, int size, long totalElements, long totalPages) {

  public ResourceSlotPageResult {
    items = List.copyOf(items);
  }

  public static ResourceSlotPageResult of(
      List<ResourceSlotResult> items, ResourceSlotPageQuery query, long totalElements) {
    long totalPages = totalElements == 0 ? 0 : ((totalElements - 1) / query.size()) + 1;
    return new ResourceSlotPageResult(items, query.page(), query.size(), totalElements, totalPages);
  }
}
