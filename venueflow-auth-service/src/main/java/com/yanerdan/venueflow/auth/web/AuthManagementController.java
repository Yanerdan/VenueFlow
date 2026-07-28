package com.yanerdan.venueflow.auth.web;

import com.yanerdan.venueflow.auth.application.AuthRoleManagementService;
import com.yanerdan.venueflow.auth.domain.AuthAccount;
import com.yanerdan.venueflow.auth.domain.CampusRole;
import com.yanerdan.venueflow.auth.web.AuthDtos.SuccessResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("persistence")
@RequestMapping("/api/v1/auth/management/accounts")
public class AuthManagementController {
  private final AuthRoleManagementService service;

  public AuthManagementController(AuthRoleManagementService service) {
    this.service = service;
  }

  @GetMapping
  public SuccessResponse<List<AccountResponse>> accounts(
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role) {
    return AuthController.success(
        service.accounts(role).stream().map(AccountResponse::from).toList(), "accounts");
  }

  @GetMapping("/approvers")
  public SuccessResponse<List<AccountResponse>> approvers(
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role) {
    return AuthController.success(
        service.approvers(role).stream().map(AccountResponse::from).toList(), "approvers");
  }

  @PatchMapping("/{userId}/role")
  public SuccessResponse<AccountResponse> changeRole(
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role,
      @RequestHeader(value = "X-User-Id", required = false) String actorUserId,
      @PathVariable UUID userId,
      @Valid @RequestBody ChangeRoleRequest request) {
    return AuthController.success(
        AccountResponse.from(
            service.changeRole(
                actorUserId, role, userId, request.role(), request.expectedVersion())),
        "role updated");
  }

  public record ChangeRoleRequest(
      @NotNull CampusRole role, @PositiveOrZero long expectedVersion) {}

  public record AccountResponse(
      UUID userId,
      String username,
      CampusRole role,
      long tokenVersion,
      long version,
      LocalDateTime updatedAt) {
    static AccountResponse from(AuthAccount account) {
      return new AccountResponse(
          account.userId(),
          account.username(),
          account.role(),
          account.tokenVersion(),
          account.version(),
          account.updatedAt());
    }
  }
}
