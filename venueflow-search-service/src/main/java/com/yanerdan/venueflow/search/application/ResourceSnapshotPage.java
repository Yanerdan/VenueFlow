package com.yanerdan.venueflow.search.application;

import java.util.List;

public record ResourceSnapshotPage(List<ResourceDocument> items, long totalElements) {

  public ResourceSnapshotPage {
    items = List.copyOf(items);
  }
}
