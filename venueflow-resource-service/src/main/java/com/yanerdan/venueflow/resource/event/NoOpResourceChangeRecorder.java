package com.yanerdan.venueflow.resource.event;

import com.yanerdan.venueflow.resource.catalog.application.ResourceResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence & !resource-events")
final class NoOpResourceChangeRecorder implements ResourceChangeRecorder {

  @Override
  public void record(ResourceResult resource) {}
}
