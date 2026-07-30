package com.yanerdan.venueflow.user.directory;

import com.yanerdan.venueflow.user.directory.OrganizationDirectoryService.OrganizationUnit;
import com.yanerdan.venueflow.user.directory.OrganizationDirectoryService.SyncCommand;
import com.yanerdan.venueflow.user.directory.OrganizationDirectoryService.SyncRun;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("persistence")
@RequestMapping("/api/v1/organizations")
public class OrganizationDirectoryController {

  private final OrganizationDirectoryService service;

  public OrganizationDirectoryController(OrganizationDirectoryService service) {
    this.service = service;
  }

  @GetMapping
  public List<OrganizationUnit> organizations(
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role,
      @RequestParam @NotBlank @Size(max = 48) String source) {
    requireAdmin(role);
    return service.organizations(source);
  }

  @GetMapping("/sync-runs")
  public List<SyncRun> runs(
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role,
      @RequestParam @NotBlank @Size(max = 48) String source) {
    requireAdmin(role);
    return service.runs(source);
  }

  @PostMapping("/sync")
  public SyncRun synchronize(
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role,
      @Valid @RequestBody SyncRequest request) {
    requireAdmin(role);
    return service.synchronize(
        new SyncCommand(
            request.source(),
            request.runKey(),
            request.mode(),
            request.units(),
            request.memberships()));
  }

  private static void requireAdmin(String role) {
    if (!"SYSTEM_ADMIN".equals(role)) {
      throw new OrganizationDirectoryForbiddenException();
    }
  }

  public record SyncRequest(
      @NotBlank @Size(max = 48) String source,
      @NotBlank @Size(max = 96) String runKey,
      @NotBlank @Size(max = 16) String mode,
      @Size(max = 500) List<OrganizationDirectoryService.UnitInput> units,
      @Size(max = 1000) List<OrganizationDirectoryService.MembershipInput> memberships) {}
}
