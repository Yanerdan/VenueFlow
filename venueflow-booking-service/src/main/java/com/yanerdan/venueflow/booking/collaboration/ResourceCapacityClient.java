package com.yanerdan.venueflow.booking.collaboration;

import java.util.Optional;

public interface ResourceCapacityClient {
  void allocate(long slotId, String operationId, int quantity);

  void release(long slotId, String operationId, int quantity);

  Optional<ResourceOperation> findOperation(long slotId, String operationId);

  record ResourceOperation(String operationId, String operationType, int quantity) {}
}
