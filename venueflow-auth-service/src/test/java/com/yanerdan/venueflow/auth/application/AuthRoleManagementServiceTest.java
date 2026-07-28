package com.yanerdan.venueflow.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanerdan.venueflow.auth.domain.AuthAccount;
import com.yanerdan.venueflow.auth.domain.CampusRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthRoleManagementServiceTest {
  private static final UUID ADMIN = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID TARGET = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 28, 12, 0);
  @Mock private AuthRepository repository;

  @Test
  void listsBoundedAccountsForSystemAdmin() {
    when(repository.listAccounts(200)).thenReturn(List.of(account(TARGET, CampusRole.APPLICANT, 1)));
    AuthRoleManagementService service = service();

    assertThat(service.accounts("SYSTEM_ADMIN")).hasSize(1);
    verify(repository).listAccounts(200);
  }

  @Test
  void returnsOnlyEligibleApproversToResourceManager() {
    when(repository.listAccounts(200))
        .thenReturn(
            List.of(
                account(TARGET, CampusRole.APPLICANT, 1),
                account(ADMIN, CampusRole.SYSTEM_ADMIN, 1)));

    assertThat(service().approvers("RESOURCE_MANAGER"))
        .extracting(AuthAccount::role)
        .containsExactly(CampusRole.SYSTEM_ADMIN);
  }

  @Test
  void promotesAccountAndReturnsAdvancedVersion() {
    AuthAccount current = account(TARGET, CampusRole.APPLICANT, 1);
    AuthAccount updated = account(TARGET, CampusRole.APPROVER, 2);
    when(repository.findAccount(TARGET))
        .thenReturn(Optional.of(current))
        .thenReturn(Optional.of(updated));
    when(repository.changeRole(TARGET, CampusRole.APPROVER, 1, NOW)).thenReturn(true);

    AuthAccount result =
        service().changeRole(ADMIN.toString(), "SYSTEM_ADMIN", TARGET, CampusRole.APPROVER, 1);

    assertThat(result.role()).isEqualTo(CampusRole.APPROVER);
    assertThat(result.version()).isEqualTo(2);
  }

  @Test
  void leavesExistingRoleUnchanged() {
    AuthAccount current = account(TARGET, CampusRole.APPROVER, 4);
    when(repository.findAccount(TARGET)).thenReturn(Optional.of(current));

    AuthAccount result =
        service().changeRole(ADMIN.toString(), "SYSTEM_ADMIN", TARGET, CampusRole.APPROVER, 4);

    assertThat(result).isEqualTo(current);
    verify(repository, never())
        .changeRole(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsNonAdministratorAndSelfDemotion() {
    assertThatThrownBy(() -> service().accounts("APPROVER"))
        .isInstanceOf(AuthException.class)
        .extracting("code")
        .isEqualTo(AuthErrorCode.AUTH_FORBIDDEN);
    assertThatThrownBy(
            () ->
                service()
                    .changeRole(
                        ADMIN.toString(),
                        "SYSTEM_ADMIN",
                        ADMIN,
                        CampusRole.APPLICANT,
                        1))
        .isInstanceOf(AuthException.class)
        .extracting("code")
        .isEqualTo(AuthErrorCode.AUTH_FORBIDDEN);
  }

  private AuthRoleManagementService service() {
    return new AuthRoleManagementService(
        repository, Clock.fixed(Instant.parse("2026-07-28T12:00:00Z"), ZoneOffset.UTC));
  }

  private static AuthAccount account(UUID id, CampusRole role, long version) {
    return new AuthAccount(id, "staff", role, version, version, NOW);
  }
}
