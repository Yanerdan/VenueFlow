package com.yanerdan.venueflow.search.web;

import com.yanerdan.venueflow.search.application.ResourceSearchPage;
import com.yanerdan.venueflow.search.application.ResourceSearchQuery;
import com.yanerdan.venueflow.search.application.SearchApplicationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Profile("search")
@RequestMapping("/api/v1/search")
public class SearchController {

  private final SearchApplicationService service;

  public SearchController(SearchApplicationService service) {
    this.service = service;
  }

  @GetMapping("/resources")
  public ResourceSearchPage search(
      @RequestParam(required = false) @Size(max = 100) String text,
      @RequestParam(required = false) @Positive Long categoryId,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") @Min(0) @Max(1000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.search(new ResourceSearchQuery(text, categoryId, status, page, size));
  }
}
