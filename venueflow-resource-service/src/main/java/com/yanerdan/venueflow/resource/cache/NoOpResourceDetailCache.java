package com.yanerdan.venueflow.resource.cache;

import com.yanerdan.venueflow.resource.catalog.application.ResourceResult;
import java.util.function.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence & !cache")
final class NoOpResourceDetailCache implements ResourceDetailCache {

  @Override
  public ResourceResult get(Long resourceId, Supplier<ResourceResult> loader) {
    return loader.get();
  }

  @Override
  public void evictAfterCommit(Long resourceId) {}
}
