package com.yanerdan.venueflow.search.application;

public interface ResourceSnapshotClient {

  ResourceDocument get(Long resourceId);

  ResourceSnapshotPage page(int page, int size);
}
