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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
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
    jdbcTemplate.update("DELETE FROM `resource_slot_allocation`");
    jdbcTemplate.update("DELETE FROM `resource_slot`");
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

    Integer slotMigrationCount =
        jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE script = 'V002__add_resource_slots.sql'
                  AND success = 1
                """,
            Integer.class);

    assertThat(slotMigrationCount).isEqualTo(1);

    Integer allocationMigrationCount =
        jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE script = 'V003__add_slot_capacity_allocation.sql'
                  AND success = 1
                """,
            Integer.class);

    assertThat(allocationMigrationCount).isEqualTo(1);

    List<String> businessTables =
        jdbcTemplate.queryForList(
            """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                      'resource_category',
                      'resource',
                      'resource_slot',
                      'resource_slot_allocation'
                  )
                """,
            String.class);

    assertThat(businessTables)
        .containsExactlyInAnyOrder(
            "resource_category", "resource", "resource_slot", "resource_slot_allocation");
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

  private void activateResource(long resourceId) throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/resources/{resourceId}/status", resourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"targetStatus\": \"ACTIVE\", \"expectedVersion\": 1 }"))
        .andExpect(status().isOk());
  }

  private MvcResult createSlot(long resourceId, String startAt, String endAt) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/resources/{resourceId}/slots", resourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "startAt": "%s",
                          "endAt": "%s"
                        }
                        """
                        .formatted(startAt, endAt)))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private ResultActions allocate(long slotId, String operationId, int quantity) throws Exception {
    return mockMvc.perform(
        post("/api/v1/resource-slots/{slotId}/allocations", slotId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                        { "operationId": "%s", "quantity": %d }
                        """
                    .formatted(operationId, quantity)));
  }

  private ResultActions release(long slotId, String operationId, int quantity) throws Exception {
    return mockMvc.perform(
        post("/api/v1/resource-slots/{slotId}/releases", slotId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                        { "operationId": "%s", "quantity": %d }
                        """
                    .formatted(operationId, quantity)));
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

  @Test
  void persistsQueriesAndTransitionsResourceSlotsAgainstMysql() throws Exception {
    long categoryId = createCategory("MEETING_ROOM", "Meeting Room");
    long resourceId = readLong(createResource("ROOM-A-101", categoryId, "Room A101"), "$.id");
    activateResource(resourceId);

    long slotId =
        readLong(
            createSlot(resourceId, "2026-07-23T18:00:00+08:00", "2026-07-23T19:00:00+08:00"),
            "$.id");

    mockMvc
        .perform(get("/api/v1/resource-slots/{slotId}", slotId))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.resourceId").value(resourceId),
            jsonPath("$.startAt").value("2026-07-23T10:00:00Z"),
            jsonPath("$.endAt").value("2026-07-23T11:00:00Z"),
            jsonPath("$.status").value("OPEN"),
            jsonPath("$.version").value(1));

    mockMvc
        .perform(
            get("/api/v1/resources/{resourceId}/slots", resourceId)
                .queryParam("from", "2026-07-23T09:30:00Z")
                .queryParam("to", "2026-07-23T10:30:00Z"))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.items", hasSize(1)),
            jsonPath("$.items[0].id").value(slotId),
            jsonPath("$.size").value(20));

    mockMvc
        .perform(
            patch("/api/v1/resource-slots/{slotId}/status", slotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"targetStatus\": \"CLOSED\", \"expectedVersion\": 1 }"))
        .andExpectAll(
            status().isOk(), jsonPath("$.status").value("CLOSED"), jsonPath("$.version").value(2));

    mockMvc
        .perform(
            patch("/api/v1/resource-slots/{slotId}/status", slotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"targetStatus\": \"OPEN\", \"expectedVersion\": 1 }"))
        .andExpectAll(status().isConflict(), jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
  }

  @Test
  void rejectsOverlappingSlotsButPermitsBoundaryAdjacentSlotsAgainstMysql() throws Exception {
    long categoryId = createCategory("MEETING_ROOM", "Meeting Room");
    long resourceId = readLong(createResource("ROOM-A-101", categoryId, "Room A101"), "$.id");
    activateResource(resourceId);
    createSlot(resourceId, "2026-07-23T10:00:00Z", "2026-07-23T11:00:00Z");

    mockMvc
        .perform(
            post("/api/v1/resources/{resourceId}/slots", resourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        { "startAt": "2026-07-23T10:30:00Z", "endAt": "2026-07-23T11:30:00Z" }
                        """))
        .andExpectAll(
            status().isConflict(), jsonPath("$.code").value("RESOURCE_SLOT_TIME_OVERLAP"));

    createSlot(resourceId, "2026-07-23T11:00:00Z", "2026-07-23T12:00:00Z");

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM resource_slot WHERE resource_id = ?", Integer.class, resourceId);
    assertThat(count).isEqualTo(2);
  }

  @Test
  void enforcesResourceSlotForeignKeyAndStatusConstraintInMysql() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                        INSERT INTO resource_slot (resource_id, start_at, end_at, status, version)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                    999999L,
                    "2026-07-23 10:00:00",
                    "2026-07-23 11:00:00",
                    "OPEN",
                    1L))
        .isInstanceOf(DataIntegrityViolationException.class);

    jdbcTemplate.update("INSERT INTO resource_category (code, name) VALUES ('DESK', 'Desk')");
    long categoryId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM resource_category WHERE code = 'DESK'", Long.class);
    jdbcTemplate.update(
        """
            INSERT INTO resource (resource_no, category_id, name, capacity, status, version)
            VALUES ('DESK-A-1', ?, 'Desk A1', 1, 'ACTIVE', 1)
            """,
        categoryId);
    long resourceId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM resource WHERE resource_no = 'DESK-A-1'", Long.class);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                        INSERT INTO resource_slot (resource_id, start_at, end_at, status, version)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                    resourceId,
                    "2026-07-23 10:00:00",
                    "2026-07-23 11:00:00",
                    "UNKNOWN",
                    1L))
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void allocatesReplaysReleasesAndQueriesCapacityAgainstMysql() throws Exception {
    long categoryId = createCategory("MEETING_ROOM", "Meeting Room");
    long resourceId = readLong(createResource("ROOM-A-101", categoryId, "Room A101"), "$.id");
    activateResource(resourceId);
    long slotId =
        readLong(createSlot(resourceId, "2026-07-23T10:00:00Z", "2026-07-23T11:00:00Z"), "$.id");

    allocate(slotId, "allocate-1", 4)
        .andExpectAll(
            status().isCreated(),
            jsonPath("$.operationType").value("ALLOCATE"),
            jsonPath("$.capacity.occupiedQuantity").value(4),
            jsonPath("$.capacity.availableQuantity").value(6));
    allocate(slotId, "allocate-1", 4)
        .andExpectAll(status().isCreated(), jsonPath("$.capacity.occupiedQuantity").value(4));
    release(slotId, "release-1", 2)
        .andExpectAll(
            status().isCreated(),
            jsonPath("$.operationType").value("RELEASE"),
            jsonPath("$.capacity.occupiedQuantity").value(2));

    mockMvc
        .perform(get("/api/v1/resource-slots/{slotId}/capacity", slotId))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.staticCapacity").value(10),
            jsonPath("$.occupiedQuantity").value(2),
            jsonPath("$.availableQuantity").value(8));
    mockMvc
        .perform(get("/api/v1/resource-slots/{slotId}/allocation-operations", slotId))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.items", hasSize(2)),
            jsonPath("$.size").value(20),
            jsonPath("$.items[0].operationId").value("allocate-1"),
            jsonPath("$.items[1].operationId").value("release-1"));

    allocate(slotId, "allocate-1", 3)
        .andExpectAll(
            status().isConflict(), jsonPath("$.code").value("ALLOCATION_OPERATION_CONFLICT"));
    release(slotId, "release-too-much", 3)
        .andExpectAll(
            status().isConflict(), jsonPath("$.code").value("RELEASE_EXCEEDS_OCCUPIED_CAPACITY"));
  }

  @Test
  void preventsConcurrentAllocationOversubscriptionAgainstMysql() throws Exception {
    long categoryId = createCategory("MEETING_ROOM", "Meeting Room");
    long resourceId = readLong(createResource("ROOM-A-101", categoryId, "Room A101"), "$.id");
    activateResource(resourceId);
    long slotId =
        readLong(createSlot(resourceId, "2026-07-23T10:00:00Z", "2026-07-23T11:00:00Z"), "$.id");

    try (ExecutorService executor = Executors.newFixedThreadPool(20)) {
      List<Callable<Integer>> requests =
          java.util.stream.IntStream.range(0, 20)
              .<Callable<Integer>>mapToObj(
                  index ->
                      () ->
                          allocate(slotId, "concurrent-" + index, 1)
                              .andReturn()
                              .getResponse()
                              .getStatus())
              .toList();
      List<Future<Integer>> results = executor.invokeAll(requests);
      long created = 0;
      for (Future<Integer> result : results) {
        if (result.get() == 201) {
          created++;
        }
      }
      assertThat(created).isEqualTo(10);
    }

    Integer occupied =
        jdbcTemplate.queryForObject(
            "SELECT allocated_quantity FROM resource_slot WHERE id = ?", Integer.class, slotId);
    Integer operationCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM resource_slot_allocation WHERE slot_id = ?",
            Integer.class,
            slotId);
    assertThat(occupied).isEqualTo(10);
    assertThat(operationCount).isEqualTo(10);
  }
}
