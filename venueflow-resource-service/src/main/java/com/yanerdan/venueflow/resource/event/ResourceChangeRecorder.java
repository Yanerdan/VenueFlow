package com.yanerdan.venueflow.resource.event;

import com.yanerdan.venueflow.resource.catalog.application.ResourceResult;

public interface ResourceChangeRecorder {

  void record(ResourceResult resource);
}
