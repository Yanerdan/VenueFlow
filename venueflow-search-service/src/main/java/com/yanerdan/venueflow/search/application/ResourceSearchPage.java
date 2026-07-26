package com.yanerdan.venueflow.search.application;

import java.util.List;

public record ResourceSearchPage(
    List<ResourceDocument> items, int page, int size, long totalElements) {

  public ResourceSearchPage {
    items = List.copyOf(items);
  }
}
