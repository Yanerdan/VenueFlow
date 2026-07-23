package com.yanerdan.venueflow.user.profile.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yanerdan.venueflow.user.profile.application.StaleUserProfileVersionException;
import com.yanerdan.venueflow.user.profile.application.UserBookingEligibilityView;
import com.yanerdan.venueflow.user.profile.application.UserProfileApplicationService;
import com.yanerdan.venueflow.user.profile.application.UserProfileNotFoundException;
import com.yanerdan.venueflow.user.profile.application.UserProfilePersistenceException;
import com.yanerdan.venueflow.user.profile.domain.AccountStatus;
import com.yanerdan.venueflow.user.profile.domain.BookingEligibility;
import com.yanerdan.venueflow.user.profile.domain.DuplicateExternalUserIdException;
import com.yanerdan.venueflow.user.profile.domain.ExternalUserId;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import com.yanerdan.venueflow.user.profile.domain.UserProfileId;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(UserProfileController.class)
@ActiveProfiles("persistence")
class UserProfileControllerTest {

  private static final long PROFILE_ID = 42L;

  private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 7, 23, 10, 30);

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserProfileApplicationService service;

  @Test
  void createsUserProfile() throws Exception {
    when(service.create("customer-123", "Alice")).thenReturn(profile());

    mockMvc
        .perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "externalUserId": "customer-123",
                          "displayName": "Alice"
                        }
                        """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/users/42"))
        .andExpect(jsonPath("$.id").value(PROFILE_ID))
        .andExpect(jsonPath("$.externalUserId").value("customer-123"))
        .andExpect(jsonPath("$.displayName").value("Alice"))
        .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.bookingEligibility").value("ELIGIBLE"))
        .andExpect(jsonPath("$.version").value(0))
        .andExpect(jsonPath("$.createdAt").value("2026-07-23T10:30:00"))
        .andExpect(jsonPath("$.updatedAt").value("2026-07-23T10:30:00"));

    verify(service).create("customer-123", "Alice");
  }

  @Test
  void retrievesUserProfile() throws Exception {
    when(service.getById(PROFILE_ID)).thenReturn(profile());

    mockMvc
        .perform(get("/api/v1/users/{userId}", PROFILE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(PROFILE_ID))
        .andExpect(jsonPath("$.externalUserId").value("customer-123"))
        .andExpect(jsonPath("$.displayName").value("Alice"))
        .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.bookingEligibility").value("ELIGIBLE"))
        .andExpect(jsonPath("$.version").value(0));

    verify(service).getById(PROFILE_ID);
  }

  @Test
  void retrievesBookingEligibility() throws Exception {
    UserBookingEligibilityView view =
        new UserBookingEligibilityView(
            PROFILE_ID, AccountStatus.ACTIVE, BookingEligibility.ELIGIBLE, true, 3L, TIMESTAMP);

    when(service.getBookingEligibility(PROFILE_ID)).thenReturn(view);

    mockMvc
        .perform(get("/api/v1/users/{userId}/booking-eligibility", PROFILE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(PROFILE_ID))
        .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.bookingEligibility").value("ELIGIBLE"))
        .andExpect(jsonPath("$.bookingPermitted").value(true))
        .andExpect(jsonPath("$.version").value(3))
        .andExpect(jsonPath("$.updatedAt").value("2026-07-23T10:30:00"));

    verify(service).getBookingEligibility(PROFILE_ID);
  }

  @Test
  void returnsSafeNotFoundEnvelope() throws Exception {
    when(service.getById(PROFILE_ID)).thenThrow(new UserProfileNotFoundException(PROFILE_ID));

    mockMvc
        .perform(get("/api/v1/users/{userId}", PROFILE_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_PROFILE_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("User profile was not found"))
        .andExpect(jsonPath("$.details.userId").value(PROFILE_ID))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  void returnsSafeDuplicateExternalIdentifierEnvelope() throws Exception {
    when(service.create("customer-123", "Alice"))
        .thenThrow(
            new DuplicateExternalUserIdException(
                new DuplicateKeyException("Duplicate entry for user_profile")));

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                              "externalUserId": "customer-123",
                              "displayName": "Alice"
                            }
                            """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("USER_PROFILE_EXTERNAL_ID_CONFLICT"))
            .andExpect(jsonPath("$.message").value("External user identifier is already in use"))
            .andExpect(jsonPath("$.details").isMap())
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andReturn();

    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("Duplicate entry", "user_profile", "DuplicateKeyException");
  }

  @Test
  void returnsStableVersionConflictEnvelope() throws Exception {
    when(service.updateDisplayName(PROFILE_ID, "Alice Chen", 3L))
        .thenThrow(new StaleUserProfileVersionException(PROFILE_ID, 3L));

    mockMvc
        .perform(
            patch("/api/v1/users/{userId}/profile", PROFILE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "displayName": "Alice Chen",
                          "expectedVersion": 3
                        }
                        """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_PROFILE_VERSION_CONFLICT"))
        .andExpect(jsonPath("$.message").value("User profile version is stale"))
        .andExpect(jsonPath("$.details.userId").value(PROFILE_ID))
        .andExpect(jsonPath("$.details.expectedVersion").value(3))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  void hidesPersistenceImplementationDetails() throws Exception {
    when(service.getById(PROFILE_ID))
        .thenThrow(
            new UserProfilePersistenceException(
                "SELECT * FROM user_profile; password=secret",
                new IllegalStateException("JDBC connection failed")));

    MvcResult result =
        mockMvc
            .perform(get("/api/v1/users/{userId}", PROFILE_ID))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("USER_PROFILE_PERSISTENCE_ERROR"))
            .andExpect(jsonPath("$.message").value("User profile operation failed"))
            .andExpect(jsonPath("$.details").isMap())
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andReturn();

    String body = result.getResponse().getContentAsString();

    assertThat(body)
        .doesNotContain(
            "SELECT",
            "user_profile",
            "password",
            "secret",
            "JDBC",
            "IllegalStateException",
            "stackTrace");
  }

  @Test
  void rejectsInvalidCreationRequestWithSafeEnvelope() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "externalUserId": "",
                          "displayName": ""
                        }
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_PROFILE_INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.details.fields.externalUserId").exists())
        .andExpect(jsonPath("$.details.fields.displayName").exists())
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  void updatesDisplayName() throws Exception {
    UserProfile updated =
        profile("Alice Chen", AccountStatus.ACTIVE, BookingEligibility.ELIGIBLE, 4L);

    when(service.updateDisplayName(PROFILE_ID, "Alice Chen", 3L)).thenReturn(updated);

    mockMvc
        .perform(
            patch("/api/v1/users/{userId}/profile", PROFILE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "displayName": "Alice Chen",
                          "expectedVersion": 3
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(PROFILE_ID))
        .andExpect(jsonPath("$.externalUserId").value("customer-123"))
        .andExpect(jsonPath("$.displayName").value("Alice Chen"))
        .andExpect(jsonPath("$.version").value(4));

    verify(service).updateDisplayName(PROFILE_ID, "Alice Chen", 3L);
  }

  @Test
  void updatesAccountStatus() throws Exception {
    UserProfile updated =
        profile("Alice", AccountStatus.SUSPENDED, BookingEligibility.ELIGIBLE, 5L);

    when(service.updateAccountStatus(PROFILE_ID, AccountStatus.SUSPENDED, 4L)).thenReturn(updated);

    mockMvc
        .perform(
            patch("/api/v1/users/{userId}/account-status", PROFILE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "accountStatus": "SUSPENDED",
                          "expectedVersion": 4
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountStatus").value("SUSPENDED"))
        .andExpect(jsonPath("$.externalUserId").value("customer-123"))
        .andExpect(jsonPath("$.version").value(5));

    verify(service).updateAccountStatus(PROFILE_ID, AccountStatus.SUSPENDED, 4L);
  }

  @Test
  void updatesBookingEligibility() throws Exception {
    UserProfile updated = profile("Alice", AccountStatus.ACTIVE, BookingEligibility.INELIGIBLE, 6L);

    when(service.updateBookingEligibility(PROFILE_ID, BookingEligibility.INELIGIBLE, 5L))
        .thenReturn(updated);

    mockMvc
        .perform(
            patch("/api/v1/users/{userId}/booking-eligibility", PROFILE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "bookingEligibility": "INELIGIBLE",
                          "expectedVersion": 5
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookingEligibility").value("INELIGIBLE"))
        .andExpect(jsonPath("$.externalUserId").value("customer-123"))
        .andExpect(jsonPath("$.version").value(6));

    verify(service).updateBookingEligibility(PROFILE_ID, BookingEligibility.INELIGIBLE, 5L);
  }

  @Test
  void rejectsDisplayNameUpdateWithoutExpectedVersion() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/users/{userId}/profile", PROFILE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "displayName": "Alice Chen"
                        }
                        """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsNegativeExpectedVersion() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/users/{userId}/account-status", PROFILE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "accountStatus": "SUSPENDED",
                          "expectedVersion": -1
                        }
                        """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsUnsupportedAccountStatusWithSafeEnvelope() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/users/{userId}/account-status", PROFILE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "accountStatus": "LOCKED",
                          "expectedVersion": 0
                        }
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_PROFILE_INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value("Request body is malformed"))
        .andExpect(jsonPath("$.details").isMap())
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  private static UserProfile profile() {
    return profile("Alice", AccountStatus.ACTIVE, BookingEligibility.ELIGIBLE, 0L);
  }

  private static UserProfile profile(
      String displayName,
      AccountStatus accountStatus,
      BookingEligibility bookingEligibility,
      long version) {
    return new UserProfile(
        new UserProfileId(PROFILE_ID),
        new ExternalUserId("customer-123"),
        displayName,
        accountStatus,
        bookingEligibility,
        version,
        TIMESTAMP,
        TIMESTAMP.plusMinutes(version));
  }
}
