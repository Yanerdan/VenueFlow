package com.yanerdan.venueflow.resource.slot.application;

import java.time.Instant;
import java.util.Objects;

public record ResourceSlotPageQuery(Long resourceId, Instant from, Instant to, int page, int size) {

  public static final int DEFAULT_PAGE = 0;
  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  public ResourceSlotPageQuery {
    if (resourceId == null || resourceId <= 0) {
      throw new IllegalArgumentException("resourceId must be positive");
    }
    Objects.requireNonNull(from, "from must not be null");
    Objects.requireNonNull(to, "to must not be null");
    if (!to.isAfter(from)) {
      throw new IllegalArgumentException("to must be after from");
    }
    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
    }
  }

  public static ResourceSlotPageQuery of(
      Long resourceId, Instant from, Instant to, Integer page, Integer size) {
    return new ResourceSlotPageQuery(
        resourceId,
        from,
        to,
        page == null ? DEFAULT_PAGE : page,
        size == null ? DEFAULT_SIZE : size);
  }

  public long offset() {
    return (long) page * size;
  }
}
