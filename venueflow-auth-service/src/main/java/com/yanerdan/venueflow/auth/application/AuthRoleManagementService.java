package com.yanerdan.venueflow.auth.application;

import com.yanerdan.venueflow.auth.domain.AuthAccount;
import com.yanerdan.venueflow.auth.domain.CampusRole;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("persistence")
public class AuthRoleManagementService {
  private static final int ACCOUNT_LIMIT = 200;
  private final AuthRepository repository;
  private final Clock clock;

  public AuthRoleManagementService(AuthRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<AuthAccount> accounts(String trustedRole) {
    requireSystemAdmin(trustedRole);
    return List.copyOf(repository.listAccounts(ACCOUNT_LIMIT));
  }

  @Transactional(readOnly = true)
  public List<AuthAccount> approvers(String trustedRole) {
    if (!CampusRole.SYSTEM_ADMIN.name().equals(trustedRole)
        && !CampusRole.RESOURCE_MANAGER.name().equals(trustedRole)) {
      throw new AuthException(
          AuthErrorCode.AUTH_FORBIDDEN, "Resource management permission is required");
    }
    return repository.listAccounts(ACCOUNT_LIMIT).stream()
        .filter(
            account ->
                account.role() == CampusRole.APPROVER
                    || account.role() == CampusRole.SYSTEM_ADMIN)
        .toList();
  }

  @Transactional
  public AuthAccount changeRole(
      String actorUserId,
      String trustedRole,
      UUID targetUserId,
      CampusRole targetRole,
      long expectedVersion) {
    requireSystemAdmin(trustedRole);
    if (targetRole == null || expectedVersion < 0) {
      throw new IllegalArgumentException("Role update is invalid");
    }
    UUID actor = parseActor(actorUserId);
    if (actor.equals(targetUserId) && targetRole != CampusRole.SYSTEM_ADMIN) {
      throw new AuthException(
          AuthErrorCode.AUTH_FORBIDDEN, "System administrators cannot demote themselves");
    }
    AuthAccount current =
        repository
            .findAccount(targetUserId)
            .orElseThrow(
                () ->
                    new AuthException(
                        AuthErrorCode.AUTH_ACCOUNT_NOT_FOUND, "Authentication account was not found"));
    if (current.role() == targetRole) return current;
    if (!repository.changeRole(
        targetUserId, targetRole, expectedVersion, LocalDateTime.now(clock.withZone(ZoneOffset.UTC)))) {
      throw new AuthException(
          AuthErrorCode.AUTH_ROLE_CONFLICT, "Authentication account changed concurrently");
    }
    return repository
        .findAccount(targetUserId)
        .orElseThrow(
            () ->
                new AuthException(
                    AuthErrorCode.AUTH_ACCOUNT_NOT_FOUND, "Authentication account was not found"));
  }

  private static void requireSystemAdmin(String role) {
    if (!CampusRole.SYSTEM_ADMIN.name().equals(role)) {
      throw new AuthException(
          AuthErrorCode.AUTH_FORBIDDEN, "System administrator permission is required");
    }
  }

  private static UUID parseActor(String value) {
    try {
      return UUID.fromString(value);
    } catch (RuntimeException exception) {
      throw new AuthException(
          AuthErrorCode.AUTH_FORBIDDEN, "Trusted administrator identity is required");
    }
  }
}
