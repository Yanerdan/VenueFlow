package com.yanerdan.venueflow.resource.cache;

import com.yanerdan.venueflow.resource.catalog.application.ResourceResult;
import java.util.function.Supplier;

public interface ResourceDetailCache {

  ResourceResult get(Long resourceId, Supplier<ResourceResult> loader);

  void evictAfterCommit(Long resourceId);
}
