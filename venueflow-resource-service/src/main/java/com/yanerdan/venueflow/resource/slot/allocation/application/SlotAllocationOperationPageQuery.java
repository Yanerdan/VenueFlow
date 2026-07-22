package com.yanerdan.venueflow.resource.slot.allocation.application;

public record SlotAllocationOperationPageQuery(Long slotId, int page, int size) {

  public static final int DEFAULT_PAGE = 0;
  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  public SlotAllocationOperationPageQuery {
    if (slotId == null || slotId <= 0) {
      throw new IllegalArgumentException("slotId must be positive");
    }
    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
    }
  }

  public static SlotAllocationOperationPageQuery of(Long slotId, Integer page, Integer size) {
    return new SlotAllocationOperationPageQuery(
        slotId, page == null ? DEFAULT_PAGE : page, size == null ? DEFAULT_SIZE : size);
  }

  public long offset() {
    return (long) page * size;
  }
}
