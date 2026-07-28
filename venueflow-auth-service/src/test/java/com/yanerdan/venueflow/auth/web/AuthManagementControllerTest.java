package com.yanerdan.venueflow.auth.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yanerdan.venueflow.auth.application.AuthErrorCode;
import com.yanerdan.venueflow.auth.application.AuthException;
import com.yanerdan.venueflow.auth.application.AuthRoleManagementService;
import com.yanerdan.venueflow.auth.domain.AuthAccount;
import com.yanerdan.venueflow.auth.domain.CampusRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthManagementController.class)
@ActiveProfiles("persistence")
class AuthManagementControllerTest {
  private static final UUID ADMIN = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID TARGET = UUID.fromString("22222222-2222-2222-2222-222222222222");
  @Autowired private MockMvc mockMvc;
  @MockitoBean private AuthRoleManagementService service;

  @Test
  void listsSafeAccounts() throws Exception {
    when(service.accounts("SYSTEM_ADMIN")).thenReturn(List.of(account(CampusRole.APPROVER, 2)));

    mockMvc
        .perform(
            get("/api/v1/auth/management/accounts").header("X-Role", "SYSTEM_ADMIN"))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.data[0].userId").value(TARGET.toString()),
            jsonPath("$.data[0].username").value("staff"),
            jsonPath("$.data[0].role").value("APPROVER"),
            jsonPath("$.data[0].passwordHash").doesNotExist());
  }

  @Test
  void listsEligibleApproversForResourceManager() throws Exception {
    when(service.approvers("RESOURCE_MANAGER"))
        .thenReturn(List.of(account(CampusRole.APPROVER, 2)));

    mockMvc
        .perform(
            get("/api/v1/auth/management/accounts/approvers")
                .header("X-Role", "RESOURCE_MANAGER"))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.data[0].role").value("APPROVER"),
            jsonPath("$.data[0].userId").value(TARGET.toString()));
  }

  @Test
  void updatesRoleAndMapsForbiddenAccess() throws Exception {
    when(service.changeRole(
            ADMIN.toString(), "SYSTEM_ADMIN", TARGET, CampusRole.APPROVER, 1))
        .thenReturn(account(CampusRole.APPROVER, 2));
    mockMvc
        .perform(
            patch("/api/v1/auth/management/accounts/{userId}/role", TARGET)
                .header("X-Role", "SYSTEM_ADMIN")
                .header("X-User-Id", ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"APPROVER\",\"expectedVersion\":1}"))
        .andExpectAll(status().isOk(), jsonPath("$.data.version").value(2));

    when(service.accounts("APPROVER"))
        .thenThrow(new AuthException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden"));
    mockMvc
        .perform(get("/api/v1/auth/management/accounts").header("X-Role", "APPROVER"))
        .andExpectAll(status().isForbidden(), jsonPath("$.code").value("AUTH_FORBIDDEN"));
  }

  private static AuthAccount account(CampusRole role, long version) {
    return new AuthAccount(
        TARGET, "staff", role, version, version, LocalDateTime.of(2026, 7, 28, 12, 0));
  }
}
