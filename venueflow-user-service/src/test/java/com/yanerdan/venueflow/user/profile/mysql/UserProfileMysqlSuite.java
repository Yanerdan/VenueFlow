package com.yanerdan.venueflow.user.profile.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("persistence")
class UserProfileMysqlSuite {

  @Container
  private static final MySQLContainer MYSQL =
      new MySQLContainer("mysql:8.4.10")
          .withDatabaseName("venueflow_user")
          .withUsername("venueflow_user_app")
          .withPassword("user-profile-test-password");

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
  }

  @BeforeEach
  void cleanUserProfiles() {
    jdbcTemplate.update("DELETE FROM user_profile");
  }

  @Test
  void migratesFreshUserProfileSchema() {
    Integer migrationCount =
        jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE script = 'V001__init_user_profile.sql'
                  AND success = 1
                """,
            Integer.class);

    List<String> businessTables =
        jdbcTemplate.queryForList(
            """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'user_profile'
                """,
            String.class);

    assertThat(migrationCount).isEqualTo(1);
    assertThat(businessTables).containsExactly("user_profile");
  }

  @Test
  void persistsAndRetrievesProfileWithDefaults() throws Exception {
    long userId = createUser("employee-001", "Alice");

    mockMvc
        .perform(get("/api/v1/users/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.externalUserId").value("employee-001"))
        .andExpect(jsonPath("$.displayName").value("Alice"))
        .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.bookingEligibility").value("ELIGIBLE"))
        .andExpect(jsonPath("$.version").value(0));

    Integer profileCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_profile WHERE external_user_id = ?",
            Integer.class,
            "employee-001");

    assertThat(profileCount).isEqualTo(1);
  }

  @Test
  void rejectsDuplicateExternalUserIdentifierWithoutPersistingAnotherProfile() throws Exception {
    createUser("employee-duplicate", "Alice");

    mockMvc
        .perform(createUserRequest("employee-duplicate", "Alice Duplicate"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_PROFILE_EXTERNAL_ID_CONFLICT"));

    Integer profileCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_profile WHERE external_user_id = ?",
            Integer.class,
            "employee-duplicate");

    assertThat(profileCount).isEqualTo(1);
  }

  @Test
  void persistsStateChangesAndReportsBookingEligibility() throws Exception {
    long userId = createUser("employee-eligibility", "Alice");

    mockMvc
        .perform(
            patch("/api/v1/users/{userId}/booking-eligibility", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "bookingEligibility": "INELIGIBLE",
                          "expectedVersion": 0
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookingEligibility").value("INELIGIBLE"))
        .andExpect(jsonPath("$.version").value(1));

    mockMvc
        .perform(get("/api/v1/users/{userId}/booking-eligibility", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.bookingEligibility").value("INELIGIBLE"))
        .andExpect(jsonPath("$.bookingPermitted").value(false))
        .andExpect(jsonPath("$.version").value(1));

    mockMvc
        .perform(
            patch("/api/v1/users/{userId}/account-status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "accountStatus": "SUSPENDED",
                          "expectedVersion": 1
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountStatus").value("SUSPENDED"))
        .andExpect(jsonPath("$.version").value(2));

    mockMvc
        .perform(get("/api/v1/users/{userId}/booking-eligibility", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookingPermitted").value(false))
        .andExpect(jsonPath("$.version").value(2));
  }

  @Test
  void rejectsStaleUpdateAndPreservesCurrentPersistedState() throws Exception {
    long userId = createUser("employee-stale", "Alice");

    mockMvc
        .perform(
            patch("/api/v1/users/{userId}/profile", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "displayName": "Alice Chen",
                          "expectedVersion": 0
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Alice Chen"))
        .andExpect(jsonPath("$.version").value(1));

    mockMvc
        .perform(
            patch("/api/v1/users/{userId}/account-status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "accountStatus": "SUSPENDED",
                          "expectedVersion": 0
                        }
                        """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_PROFILE_VERSION_CONFLICT"));

    mockMvc
        .perform(get("/api/v1/users/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Alice Chen"))
        .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.version").value(1));
  }

  private long createUser(String externalUserId, String displayName) throws Exception {
    MvcResult result = mockMvc.perform(createUserRequest(externalUserId, displayName)).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(201);

    Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

    return id.longValue();
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
      createUserRequest(String externalUserId, String displayName) {
    return post("/api/v1/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
                {
                  "externalUserId": "%s",
                  "displayName": "%s"
                }
                """
                .formatted(externalUserId, displayName));
  }
}
