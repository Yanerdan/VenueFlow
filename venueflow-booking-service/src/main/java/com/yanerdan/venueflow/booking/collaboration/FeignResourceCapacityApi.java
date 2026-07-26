package com.yanerdan.venueflow.booking.collaboration;

import java.time.Instant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "venueflow-resource-service", contextId = "resourceCapacity")
interface FeignResourceCapacityApi {

  @PostMapping("/api/v1/resource-slots/{slotId}/allocations")
  void allocate(@PathVariable long slotId, @RequestBody CapacityChange request);

  @PostMapping("/api/v1/resource-slots/{slotId}/releases")
  void release(@PathVariable long slotId, @RequestBody CapacityChange request);

  @GetMapping("/api/v1/resource-slots/{slotId}/allocation-operations/{operationId}")
  OperationResponse operation(@PathVariable long slotId, @PathVariable String operationId);

  @GetMapping("/api/v1/resource-slots/{slotId}")
  SlotResponse slot(@PathVariable long slotId);

  record CapacityChange(String operationId, int quantity) {}

  record OperationResponse(String operationId, String operationType, int quantity) {}

  record SlotResponse(long id, Instant startAt, Instant endAt) {}
}
