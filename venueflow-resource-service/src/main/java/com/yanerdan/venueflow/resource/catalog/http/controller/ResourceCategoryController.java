package com.yanerdan.venueflow.resource.catalog.http.controller;

import com.yanerdan.venueflow.resource.catalog.application.CatalogApplicationService;
import com.yanerdan.venueflow.resource.catalog.http.request.CreateCategoryRequest;
import com.yanerdan.venueflow.resource.catalog.http.response.CategoryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("persistence")
@RequestMapping("/api/v1/resource-categories")
public class ResourceCategoryController {

  private final CatalogApplicationService catalogApplicationService;

  public ResourceCategoryController(CatalogApplicationService catalogApplicationService) {
    this.catalogApplicationService = catalogApplicationService;
  }

  @PostMapping
  public ResponseEntity<CategoryResponse> createCategory(
      @Valid @RequestBody CreateCategoryRequest request) {
    CategoryResponse response =
        CategoryResponse.from(catalogApplicationService.createCategory(request.toCommand()));

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public List<CategoryResponse> listCategories() {
    return catalogApplicationService.listCategories().stream().map(CategoryResponse::from).toList();
  }
}
