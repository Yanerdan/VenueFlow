package com.yanerdan.venueflow.resource.slot.http.response;

import com.yanerdan.venueflow.resource.slot.application.ResourceSlotPageResult;
import java.util.List;

public record ResourceSlotPageResponse(
    List<ResourceSlotResponse> items, int page, int size, long totalElements, long totalPages) {

  public ResourceSlotPageResponse {
    items = List.copyOf(items);
  }

  public static ResourceSlotPageResponse from(ResourceSlotPageResult result) {
    return new ResourceSlotPageResponse(
        result.items().stream().map(ResourceSlotResponse::from).toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages());
  }
}
