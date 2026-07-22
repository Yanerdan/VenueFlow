package com.yanerdan.venueflow.resource.slot.allocation.http.controller;

import com.yanerdan.venueflow.resource.slot.allocation.application.SlotCapacityApplicationService;
import com.yanerdan.venueflow.resource.slot.allocation.http.request.SlotAllocationOperationPageRequest;
import com.yanerdan.venueflow.resource.slot.allocation.http.request.SlotCapacityChangeRequest;
import com.yanerdan.venueflow.resource.slot.allocation.http.response.SlotAllocationOperationPageResponse;
import com.yanerdan.venueflow.resource.slot.allocation.http.response.SlotCapacityChangeResponse;
import com.yanerdan.venueflow.resource.slot.allocation.http.response.SlotCapacityResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("persistence")
public class SlotCapacityController {

  private final SlotCapacityApplicationService slotCapacityApplicationService;

  public SlotCapacityController(SlotCapacityApplicationService slotCapacityApplicationService) {
    this.slotCapacityApplicationService = slotCapacityApplicationService;
  }

  @PostMapping("/api/v1/resource-slots/{slotId}/allocations")
  public ResponseEntity<SlotCapacityChangeResponse> allocate(
      @PathVariable @Positive(message = "slotId must be positive") Long slotId,
      @Valid @RequestBody SlotCapacityChangeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            SlotCapacityChangeResponse.from(
                slotCapacityApplicationService.allocate(request.toCommand(slotId))));
  }

  @PostMapping("/api/v1/resource-slots/{slotId}/releases")
  public ResponseEntity<SlotCapacityChangeResponse> release(
      @PathVariable @Positive(message = "slotId must be positive") Long slotId,
      @Valid @RequestBody SlotCapacityChangeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            SlotCapacityChangeResponse.from(
                slotCapacityApplicationService.release(request.toCommand(slotId))));
  }

  @GetMapping("/api/v1/resource-slots/{slotId}/capacity")
  public SlotCapacityResponse getCapacity(
      @PathVariable @Positive(message = "slotId must be positive") Long slotId) {
    return SlotCapacityResponse.from(slotCapacityApplicationService.getCapacity(slotId));
  }

  @GetMapping("/api/v1/resource-slots/{slotId}/allocation-operations")
  public SlotAllocationOperationPageResponse listOperations(
      @PathVariable @Positive(message = "slotId must be positive") Long slotId,
      @Valid @ModelAttribute SlotAllocationOperationPageRequest request) {
    return SlotAllocationOperationPageResponse.from(
        slotCapacityApplicationService.listOperations(request.toQuery(slotId)));
  }
}
