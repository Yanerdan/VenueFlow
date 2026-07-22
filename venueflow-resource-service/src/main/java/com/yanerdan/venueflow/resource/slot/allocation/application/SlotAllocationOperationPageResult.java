package com.yanerdan.venueflow.resource.slot.allocation.application;

import java.util.List;

public record SlotAllocationOperationPageResult(
    List<SlotAllocationOperationResult> items,
    int page,
    int size,
    long totalElements,
    long totalPages) {

  public SlotAllocationOperationPageResult {
    items = List.copyOf(items);
  }

  static SlotAllocationOperationPageResult of(
      List<SlotAllocationOperationResult> items,
      SlotAllocationOperationPageQuery query,
      long totalElements) {
    long totalPages = totalElements == 0 ? 0 : ((totalElements - 1) / query.size()) + 1;
    return new SlotAllocationOperationPageResult(
        items, query.page(), query.size(), totalElements, totalPages);
  }
}
