package com.yanerdan.venueflow.resource.catalog.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ResourceStatusTransitions {

  private static final Map<ResourceStatus, Set<ResourceStatus>> ALLOWED_TRANSITIONS =
      Map.of(
          ResourceStatus.DRAFT,
          Set.of(ResourceStatus.ACTIVE, ResourceStatus.ARCHIVED),
          ResourceStatus.ACTIVE,
          Set.of(ResourceStatus.SUSPENDED, ResourceStatus.ARCHIVED),
          ResourceStatus.SUSPENDED,
          Set.of(ResourceStatus.ACTIVE, ResourceStatus.ARCHIVED),
          ResourceStatus.ARCHIVED,
          Set.of());

  private ResourceStatusTransitions() {}

  public static boolean canTransition(ResourceStatus currentStatus, ResourceStatus targetStatus) {
    Objects.requireNonNull(currentStatus, "currentStatus must not be null");

    Objects.requireNonNull(targetStatus, "targetStatus must not be null");

    return ALLOWED_TRANSITIONS.get(currentStatus).contains(targetStatus);
  }
}
