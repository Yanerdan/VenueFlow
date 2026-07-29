package com.yanerdan.venueflow.resource.catalog.http.controller;

import com.yanerdan.venueflow.resource.catalog.application.CatalogApplicationService;
import com.yanerdan.venueflow.resource.catalog.http.request.ChangeResourceBookingRulesRequest;
import com.yanerdan.venueflow.resource.catalog.http.request.ChangeResourceFactsRequest;
import com.yanerdan.venueflow.resource.catalog.http.request.ChangeResourceOwnershipRequest;
import com.yanerdan.venueflow.resource.catalog.http.request.ChangeResourceStatusRequest;
import com.yanerdan.venueflow.resource.catalog.http.request.CreateResourceRequest;
import com.yanerdan.venueflow.resource.catalog.http.request.ResourcePageRequest;
import com.yanerdan.venueflow.resource.catalog.http.response.ResourcePageResponse;
import com.yanerdan.venueflow.resource.catalog.http.response.ResourceResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("persistence")
@RequestMapping("/api/v1/resources")
public class ResourceController {

  private final CatalogApplicationService catalogApplicationService;

  public ResourceController(CatalogApplicationService catalogApplicationService) {
    this.catalogApplicationService = catalogApplicationService;
  }

  @PostMapping
  public ResponseEntity<ResourceResponse> createResource(
      @Valid @RequestBody CreateResourceRequest request) {
    ResourceResponse response =
        ResourceResponse.from(catalogApplicationService.createResource(request.toCommand()));

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{resourceId}")
  public ResourceResponse getResource(
      @PathVariable @Positive(message = "resourceId must be positive") Long resourceId) {
    return ResourceResponse.from(catalogApplicationService.getResource(resourceId));
  }

  @GetMapping
  public ResourcePageResponse listResources(@Valid @ModelAttribute ResourcePageRequest request) {
    return ResourcePageResponse.from(catalogApplicationService.listResources(request.toQuery()));
  }

  @PatchMapping("/{resourceId}/status")
  public ResourceResponse changeResourceStatus(
      @PathVariable @Positive(message = "resourceId must be positive") Long resourceId,
      @Valid @RequestBody ChangeResourceStatusRequest request) {
    return ResourceResponse.from(
        catalogApplicationService.changeResourceStatus(request.toCommand(resourceId)));
  }

  @PatchMapping("/{resourceId}/ownership")
  public ResourceResponse changeResourceOwnership(
      @PathVariable @Positive(message = "resourceId must be positive") Long resourceId,
      @Valid @RequestBody ChangeResourceOwnershipRequest request) {
    return ResourceResponse.from(
        catalogApplicationService.changeResourceOwnership(request.toCommand(resourceId)));
  }

  @PatchMapping("/{resourceId}/booking-rules")
  public ResourceResponse changeResourceBookingRules(
      @PathVariable @Positive(message = "resourceId must be positive") Long resourceId,
      @Valid @RequestBody ChangeResourceBookingRulesRequest request) {
    return ResourceResponse.from(
        catalogApplicationService.changeResourceBookingRules(request.toCommand(resourceId)));
  }

  @PatchMapping("/{resourceId}/facts")
  public ResourceResponse changeResourceFacts(
      @PathVariable @Positive(message = "resourceId must be positive") Long resourceId,
      @Valid @RequestBody ChangeResourceFactsRequest request) {
    return ResourceResponse.from(
        catalogApplicationService.changeResourceFacts(request.toCommand(resourceId)));
  }
}
