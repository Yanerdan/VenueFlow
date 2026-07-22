package com.yanerdan.venueflow.resource.slot.allocation.http.response;

import com.yanerdan.venueflow.resource.slot.allocation.application.SlotAllocationOperationPageResult;
import java.util.List;

public record SlotAllocationOperationPageResponse(
    List<SlotAllocationOperationResponse> items,
    int page,
    int size,
    long totalElements,
    long totalPages) {

  public SlotAllocationOperationPageResponse {
    items = List.copyOf(items);
  }

  public static SlotAllocationOperationPageResponse from(SlotAllocationOperationPageResult result) {
    return new SlotAllocationOperationPageResponse(
        result.items().stream().map(SlotAllocationOperationResponse::from).toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages());
  }
}
