package com.yanerdan.venueflow.resource.catalog.http.response;

import com.yanerdan.venueflow.resource.catalog.application.ResourcePageResult;
import java.util.List;

public record ResourcePageResponse(
    List<ResourceResponse> items, int page, int size, long totalElements, long totalPages) {

  public ResourcePageResponse {
    items = List.copyOf(items);
  }

  public static ResourcePageResponse from(ResourcePageResult result) {
    List<ResourceResponse> responses = result.items().stream().map(ResourceResponse::from).toList();

    return new ResourcePageResponse(
        responses, result.page(), result.size(), result.totalElements(), result.totalPages());
  }
}
