package com.yanerdan.venueflow.resource.catalog.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
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
class ResourceCatalogMysqlSuite {

  @Container
  private static final MySQLContainer MYSQL =
      new MySQLContainer("mysql:8.4.10")
          .withDatabaseName("venueflow_resource")
          .withUsername("venueflow_resource_app")
          .withPassword("resource-test-password");

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
  }

  @BeforeEach
  void cleanBusinessTables() {
    jdbcTemplate.update("DELETE FROM `resource`");
    jdbcTemplate.update("DELETE FROM `resource_category`");
  }

  @Test
  void migratesFreshResourceCatalogSchema() {
    Integer migrationCount =
        jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE script = 'V001__init_resource_catalog.sql'
                  AND success = 1
                """,
            Integer.class);

    assertThat(migrationCount).isEqualTo(1);

    List<String> businessTables =
        jdbcTemplate.queryForList(
            """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                      'resource_category',
                      'resource'
                  )
                """,
            String.class);

    assertThat(businessTables).containsExactlyInAnyOrder("resource_category", "resource");
  }

  private long createCategory(String code, String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/resource-categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                              "code": "%s",
                              "name": "%s"
                            }
                            """
                            .formatted(code, name)))
            .andExpect(status().isCreated())
            .andReturn();

    return readLong(result, "$.id");
  }

  private MvcResult createResource(String resourceNo, long categoryId, String name)
      throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "resourceNo": "%s",
                          "categoryId": %d,
                          "name": "%s",
                          "description": "Integration test resource",
                          "location": "Test Building",
                          "capacity": 10
                        }
                        """
                        .formatted(resourceNo, categoryId, name)))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private static long readLong(MvcResult result, String path) throws Exception {
    String responseBody = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

    Number value = JsonPath.read(responseBody, path);

    return value.longValue();
  }

  @Test
  void createsAndRetrievesCategoryAndResource() throws Exception {
    long categoryId = createCategory("MEETING_ROOM", "Meeting Room");

    MvcResult createResult = createResource("ROOM-A-101", categoryId, "Room A101");

    long resourceId = readLong(createResult, "$.id");

    mockMvc
        .perform(get("/api/v1/resources/{resourceId}", resourceId))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.id").value(resourceId),
            jsonPath("$.resourceNo").value("ROOM-A-101"),
            jsonPath("$.categoryId").value(categoryId),
            jsonPath("$.name").value("Room A101"),
            jsonPath("$.capacity").value(10),
            jsonPath("$.status").value("DRAFT"),
            jsonPath("$.version").value(1),
            jsonPath("$.createdAt").exists(),
            jsonPath("$.updatedAt").exists());

    mockMvc
        .perform(
            get("/api/v1/resources")
                .queryParam("categoryId", Long.toString(categoryId))
                .queryParam("status", "DRAFT"))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.items", hasSize(1)),
            jsonPath("$.page").value(0),
            jsonPath("$.size").value(20),
            jsonPath("$.totalElements").value(1),
            jsonPath("$.items[0].resourceNo").value("ROOM-A-101"));
  }

  @Test
  void rejectsDuplicateResourceNumberAndKeepsExistingResource() throws Exception {
    long categoryId = createCategory("MEETING_ROOM", "Meeting Room");

    MvcResult originalResult = createResource("ROOM-A-101", categoryId, "Original Room");

    long resourceId = readLong(originalResult, "$.id");

    mockMvc
        .perform(
            post("/api/v1/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "resourceNo": "ROOM-A-101",
                          "categoryId": %d,
                          "name": "Replacement Room",
                          "capacity": 20
                        }
                        """
                        .formatted(categoryId)))
        .andExpectAll(
            status().isConflict(), jsonPath("$.code").value("RESOURCE_NUMBER_ALREADY_EXISTS"));

    mockMvc
        .perform(get("/api/v1/resources/{resourceId}", resourceId))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.name").value("Original Room"),
            jsonPath("$.capacity").value(10),
            jsonPath("$.status").value("DRAFT"),
            jsonPath("$.version").value(1));

    Integer count =
        jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM `resource`
                WHERE resource_no = ?
                """,
            Integer.class,
            "ROOM-A-101");

    assertThat(count).isEqualTo(1);
  }

  @Test
  void enforcesCategoryForeignKeyInMysql() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO `resource` (
                        resource_no,
                        category_id,
                        name,
                        capacity,
                        status,
                        version
                    )
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    "ROOM-NO-CATEGORY",
                    999999L,
                    "Invalid Resource",
                    10,
                    "DRAFT",
                    1L))
        .isInstanceOf(DataIntegrityViolationException.class);

    Integer count =
        jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM `resource`
                WHERE resource_no = ?
                """,
            Integer.class,
            "ROOM-NO-CATEGORY");

    assertThat(count).isZero();
  }

  @Test
  void advancesVersionAndRejectsStaleStatusUpdate() throws Exception {
    long categoryId = createCategory("MEETING_ROOM", "Meeting Room");

    MvcResult createResult = createResource("ROOM-A-101", categoryId, "Room A101");

    long resourceId = readLong(createResult, "$.id");

    mockMvc
        .perform(
            patch("/api/v1/resources/{resourceId}/status", resourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "targetStatus": "ACTIVE",
                          "expectedVersion": 1
                        }
                        """))
        .andExpectAll(
            status().isOk(), jsonPath("$.status").value("ACTIVE"), jsonPath("$.version").value(2));

    mockMvc
        .perform(
            patch("/api/v1/resources/{resourceId}/status", resourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "targetStatus": "ARCHIVED",
                          "expectedVersion": 1
                        }
                        """))
        .andExpectAll(
            status().isConflict(),
            jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"),
            jsonPath("$.details.expectedVersion").value(1),
            jsonPath("$.details.actualVersion").value(2));

    mockMvc
        .perform(get("/api/v1/resources/{resourceId}", resourceId))
        .andExpectAll(
            status().isOk(), jsonPath("$.status").value("ACTIVE"), jsonPath("$.version").value(2));
  }
}
