package com.yanerdan.venueflow.resource.slot.http.controller;

import com.yanerdan.venueflow.resource.slot.application.ResourceSlotApplicationService;
import com.yanerdan.venueflow.resource.slot.http.request.ChangeResourceSlotStatusRequest;
import com.yanerdan.venueflow.resource.slot.http.request.CreateResourceSlotRequest;
import com.yanerdan.venueflow.resource.slot.http.request.ResourceSlotPageRequest;
import com.yanerdan.venueflow.resource.slot.http.response.ResourceSlotPageResponse;
import com.yanerdan.venueflow.resource.slot.http.response.ResourceSlotResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("persistence")
public class ResourceSlotController {

  private final ResourceSlotApplicationService resourceSlotApplicationService;

  public ResourceSlotController(ResourceSlotApplicationService resourceSlotApplicationService) {
    this.resourceSlotApplicationService = resourceSlotApplicationService;
  }

  @PostMapping("/api/v1/resources/{resourceId}/slots")
  public ResponseEntity<ResourceSlotResponse> createSlot(
      @PathVariable @Positive(message = "resourceId must be positive") Long resourceId,
      @Valid @RequestBody CreateResourceSlotRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ResourceSlotResponse.from(
                resourceSlotApplicationService.createSlot(request.toCommand(resourceId))));
  }

  @GetMapping("/api/v1/resources/{resourceId}/slots")
  public ResourceSlotPageResponse listSlots(
      @PathVariable @Positive(message = "resourceId must be positive") Long resourceId,
      @Valid @ModelAttribute ResourceSlotPageRequest request) {
    return ResourceSlotPageResponse.from(
        resourceSlotApplicationService.listSlots(request.toQuery(resourceId)));
  }

  @GetMapping("/api/v1/resource-slots/{slotId}")
  public ResourceSlotResponse getSlot(
      @PathVariable @Positive(message = "slotId must be positive") Long slotId) {
    return ResourceSlotResponse.from(resourceSlotApplicationService.getSlot(slotId));
  }

  @PatchMapping("/api/v1/resource-slots/{slotId}/status")
  public ResourceSlotResponse changeSlotStatus(
      @PathVariable @Positive(message = "slotId must be positive") Long slotId,
      @Valid @RequestBody ChangeResourceSlotStatusRequest request) {
    return ResourceSlotResponse.from(
        resourceSlotApplicationService.changeSlotStatus(request.toCommand(slotId)));
  }
}
