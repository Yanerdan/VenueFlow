package com.yanerdan.venueflow.booking.collaboration;

import java.time.Instant;
import java.util.Optional;

public interface ResourceCapacityClient {
  void allocate(long slotId, String operationId, int quantity);

  void release(long slotId, String operationId, int quantity);

  Optional<ResourceOperation> findOperation(long slotId, String operationId);

  ResourceSlot findSlot(long slotId);

  record ResourceOperation(String operationId, String operationType, int quantity) {}

  record ResourceSlot(
      long slotId,
      Long resourceId,
      String ownerDepartment,
      String approverExternalUserId,
      Instant startAt,
      Instant endAt) {
    public ResourceSlot {
      if (slotId <= 0 || startAt == null || endAt == null || !endAt.isAfter(startAt)) {
        throw new IllegalArgumentException("Resource slot facts are invalid");
      }
    }

    public ResourceSlot(long slotId, Instant startAt, Instant endAt) {
      this(slotId, null, null, null, startAt, endAt);
    }
  }
}
