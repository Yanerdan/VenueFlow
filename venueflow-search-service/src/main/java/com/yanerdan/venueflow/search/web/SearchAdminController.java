package com.yanerdan.venueflow.search.web;

import com.yanerdan.venueflow.search.application.SearchApplicationService;
import com.yanerdan.venueflow.search.application.SearchApplicationService.RebuildResult;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("search")
@RequestMapping("/api/v1/admin/search")
public final class SearchAdminController {

  private final SearchApplicationService service;

  public SearchAdminController(SearchApplicationService service) {
    this.service = service;
  }

  @PostMapping("/rebuild")
  public RebuildResult rebuild() {
    return service.rebuild();
  }
}
