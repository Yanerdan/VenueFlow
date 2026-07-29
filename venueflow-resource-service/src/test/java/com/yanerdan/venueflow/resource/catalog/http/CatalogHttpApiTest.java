package com.yanerdan.venueflow.resource.catalog.http;

import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.CATALOG_PERSISTENCE_ERROR;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_NOT_FOUND;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_NUMBER_ALREADY_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yanerdan.venueflow.resource.catalog.application.CatalogApplicationService;
import com.yanerdan.venueflow.resource.catalog.application.CategoryResult;
import com.yanerdan.venueflow.resource.catalog.application.ChangeResourceBookingRulesCommand;
import com.yanerdan.venueflow.resource.catalog.application.ChangeResourceFactsCommand;
import com.yanerdan.venueflow.resource.catalog.application.ChangeResourceStatusCommand;
import com.yanerdan.venueflow.resource.catalog.application.CreateCategoryCommand;
import com.yanerdan.venueflow.resource.catalog.application.CreateResourceCommand;
import com.yanerdan.venueflow.resource.catalog.application.ResourcePageQuery;
import com.yanerdan.venueflow.resource.catalog.application.ResourcePageResult;
import com.yanerdan.venueflow.resource.catalog.application.ResourceResult;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import com.yanerdan.venueflow.resource.catalog.http.controller.ResourceCategoryController;
import com.yanerdan.venueflow.resource.catalog.http.controller.ResourceController;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {ResourceCategoryController.class, ResourceController.class})
@ActiveProfiles("persistence")
class CatalogHttpApiTest {

  private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 7, 21, 22, 30);

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CatalogApplicationService catalogApplicationService;

  // 测试方法放在这里
  @Test
  void createsCategoryThroughPublicDtoBoundary() throws Exception {
    CategoryResult applicationResult =
        new CategoryResult(1L, "MEETING_ROOM", "Meeting Room", TIMESTAMP, TIMESTAMP);

    when(catalogApplicationService.createCategory(any(CreateCategoryCommand.class)))
        .thenReturn(applicationResult);

    mockMvc
        .perform(
            post("/api/v1/resource-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "code": "MEETING_ROOM",
                          "name": "Meeting Room"
                        }
                        """))
        .andExpectAll(
            status().isCreated(),
            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
            jsonPath("$.*", hasSize(5)),
            jsonPath("$.id").value(1),
            jsonPath("$.code").value("MEETING_ROOM"),
            jsonPath("$.name").value("Meeting Room"),
            jsonPath("$.createdAt").exists(),
            jsonPath("$.updatedAt").exists(),
            jsonPath("$.entity").doesNotExist());

    ArgumentCaptor<CreateCategoryCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateCategoryCommand.class);

    verify(catalogApplicationService).createCategory(commandCaptor.capture());

    assertThat(commandCaptor.getValue().code()).isEqualTo("MEETING_ROOM");

    assertThat(commandCaptor.getValue().name()).isEqualTo("Meeting Room");
  }

  @Test
  void listsCategoriesUsingResponseDtos() throws Exception {
    when(catalogApplicationService.listCategories())
        .thenReturn(
            List.of(
                new CategoryResult(1L, "DESK", "Desk", TIMESTAMP, TIMESTAMP),
                new CategoryResult(2L, "MEETING_ROOM", "Meeting Room", TIMESTAMP, TIMESTAMP)));

    mockMvc
        .perform(get("/api/v1/resource-categories"))
        .andExpectAll(
            status().isOk(),
            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
            jsonPath("$", hasSize(2)),
            jsonPath("$[0].id").value(1),
            jsonPath("$[0].code").value("DESK"),
            jsonPath("$[1].id").value(2),
            jsonPath("$[1].code").value("MEETING_ROOM"),
            jsonPath("$[0].resourceCategoryEntity").doesNotExist());
  }

  @Test
  void createsResourceAndReturnsDocumentedDtoShape() throws Exception {
    ResourceResult applicationResult = resourceResult(ResourceStatus.DRAFT, 1L);

    when(catalogApplicationService.createResource(any(CreateResourceCommand.class)))
        .thenReturn(applicationResult);

    mockMvc
        .perform(
            post("/api/v1/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "resourceNo": "ROOM-A-101",
                          "categoryId": 10,
                          "name": "Room A101",
                          "description": "Meeting room",
                          "location": "Building A",
                          "capacity": 10
                        }
                        """))
        .andExpectAll(
            status().isCreated(),
            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
            jsonPath("$.*", hasSize(19)),
            jsonPath("$.id").value(100),
            jsonPath("$.resourceNo").value("ROOM-A-101"),
            jsonPath("$.categoryId").value(10),
            jsonPath("$.name").value("Room A101"),
            jsonPath("$.description").value("Meeting room"),
            jsonPath("$.location").value("Building A"),
            jsonPath("$.capacity").value(10),
            jsonPath("$.minAdvanceHours").value(0),
            jsonPath("$.maxAdvanceDays").value(90),
            jsonPath("$.maxDurationMinutes").value(480),
            jsonPath("$.status").value("DRAFT"),
            jsonPath("$.version").value(1),
            jsonPath("$.createdAt").exists(),
            jsonPath("$.updatedAt").exists(),
            jsonPath("$.resourceEntity").doesNotExist());

    ArgumentCaptor<CreateResourceCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateResourceCommand.class);

    verify(catalogApplicationService).createResource(commandCaptor.capture());

    assertThat(commandCaptor.getValue().resourceNo()).isEqualTo("ROOM-A-101");

    assertThat(commandCaptor.getValue().categoryId()).isEqualTo(10L);

    assertThat(commandCaptor.getValue().capacity()).isEqualTo(10);
  }

  @Test
  void retrievesResourceDetailAsResponseDto() throws Exception {
    when(catalogApplicationService.getResource(100L))
        .thenReturn(resourceResult(ResourceStatus.ACTIVE, 2L));

    mockMvc
        .perform(get("/api/v1/resources/{resourceId}", 100L))
        .andExpectAll(
            status().isOk(),
            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
            jsonPath("$.id").value(100),
            jsonPath("$.resourceNo").value("ROOM-A-101"),
            jsonPath("$.status").value("ACTIVE"),
            jsonPath("$.version").value(2),
            jsonPath("$.resourceEntity").doesNotExist());

    verify(catalogApplicationService).getResource(100L);
  }

  @Test
  void returnsResourcePageUsingDocumentedShape() throws Exception {
    ResourcePageResult applicationResult =
        new ResourcePageResult(List.of(resourceResult(ResourceStatus.ACTIVE, 2L)), 0, 20, 1L, 1L);

    when(catalogApplicationService.listResources(any(ResourcePageQuery.class)))
        .thenReturn(applicationResult);

    mockMvc
        .perform(get("/api/v1/resources"))
        .andExpectAll(
            status().isOk(),
            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
            jsonPath("$.*", hasSize(5)),
            jsonPath("$.items", hasSize(1)),
            jsonPath("$.page").value(0),
            jsonPath("$.size").value(20),
            jsonPath("$.totalElements").value(1),
            jsonPath("$.totalPages").value(1),
            jsonPath("$.items[0].resourceNo").value("ROOM-A-101"),
            jsonPath("$.items[0].status").value("ACTIVE"));
  }

  @Test
  void changesResourceStatusThroughCommandBoundary() throws Exception {
    when(catalogApplicationService.changeResourceStatus(any(ChangeResourceStatusCommand.class)))
        .thenReturn(resourceResult(ResourceStatus.ACTIVE, 2L));

    mockMvc
        .perform(
            patch("/api/v1/resources/{resourceId}/status", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "targetStatus": "ACTIVE",
                          "expectedVersion": 1
                        }
                        """))
        .andExpectAll(
            status().isOk(),
            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
            jsonPath("$.id").value(100),
            jsonPath("$.status").value("ACTIVE"),
            jsonPath("$.version").value(2));

    ArgumentCaptor<ChangeResourceStatusCommand> commandCaptor =
        ArgumentCaptor.forClass(ChangeResourceStatusCommand.class);

    verify(catalogApplicationService).changeResourceStatus(commandCaptor.capture());

    assertThat(commandCaptor.getValue().resourceId()).isEqualTo(100L);

    assertThat(commandCaptor.getValue().targetStatus()).isEqualTo(ResourceStatus.ACTIVE);

    assertThat(commandCaptor.getValue().expectedVersion()).isEqualTo(1L);
  }

  @Test
  void changesResourceBookingRulesThroughCommandBoundary() throws Exception {
    when(catalogApplicationService.changeResourceBookingRules(
            any(ChangeResourceBookingRulesCommand.class)))
        .thenReturn(resourceResult(ResourceStatus.ACTIVE, 3L));

    mockMvc
        .perform(
            patch("/api/v1/resources/{resourceId}/booking-rules", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "bookingNotice": "Bring a campus card",
                      "minAdvanceHours": 2,
                      "maxAdvanceDays": 30,
                      "maxDurationMinutes": 120,
                      "expectedVersion": 2
                    }
                    """))
        .andExpectAll(status().isOk(), jsonPath("$.id").value(100), jsonPath("$.version").value(3));

    ArgumentCaptor<ChangeResourceBookingRulesCommand> captor =
        ArgumentCaptor.forClass(ChangeResourceBookingRulesCommand.class);
    verify(catalogApplicationService).changeResourceBookingRules(captor.capture());
    assertThat(captor.getValue().bookingNotice()).isEqualTo("Bring a campus card");
    assertThat(captor.getValue().maxDurationMinutes()).isEqualTo(120);
  }

  @Test
  void changesResourceFactsThroughCommandBoundary() throws Exception {
    when(catalogApplicationService.changeResourceFacts(any(ChangeResourceFactsCommand.class)))
        .thenReturn(resourceResult(ResourceStatus.ACTIVE, 3L));

    mockMvc
        .perform(
            patch("/api/v1/resources/{resourceId}/facts", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": 10,
                      "name": "Room A101",
                      "description": "Meeting room",
                      "location": "Building A",
                      "capacity": 20,
                      "expectedVersion": 2
                    }
                    """))
        .andExpectAll(status().isOk(), jsonPath("$.id").value(100), jsonPath("$.version").value(3));

    ArgumentCaptor<ChangeResourceFactsCommand> captor =
        ArgumentCaptor.forClass(ChangeResourceFactsCommand.class);
    verify(catalogApplicationService).changeResourceFacts(captor.capture());
    assertThat(captor.getValue().capacity()).isEqualTo(20);
    assertThat(captor.getValue().location()).isEqualTo("Building A");
  }

  @Test
  void returnsStableValidationErrorEnvelope() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "resourceNo": "",
                          "categoryId": 10,
                          "name": "Room A101",
                          "capacity": 0
                        }
                        """))
        .andExpectAll(
            status().isBadRequest(),
            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
            header().exists("X-Trace-Id"),
            jsonPath("$.*", hasSize(5)),
            jsonPath("$.code").value("VALIDATION_ERROR"),
            jsonPath("$.message").value("Request validation failed"),
            jsonPath("$.details.fields.resourceNo").value("resourceNo must not be blank"),
            jsonPath("$.details.fields.capacity").value("capacity must be positive"),
            jsonPath("$.traceId").isNotEmpty(),
            jsonPath("$.timestamp").exists(),
            jsonPath("$.exception").doesNotExist(),
            jsonPath("$.stackTrace").doesNotExist());
  }

  @Test
  void mapsResourceNotFoundToStable404Envelope() throws Exception {
    when(catalogApplicationService.getResource(999L))
        .thenThrow(
            new CatalogException(
                RESOURCE_NOT_FOUND, "Resource was not found", Map.of("resourceId", 999L), null));

    mockMvc
        .perform(get("/api/v1/resources/{resourceId}", 999L))
        .andExpectAll(
            status().isNotFound(),
            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
            header().exists("X-Trace-Id"),
            jsonPath("$.*", hasSize(5)),
            jsonPath("$.code").value("RESOURCE_NOT_FOUND"),
            jsonPath("$.message").value("Resource was not found"),
            jsonPath("$.details.resourceId").value(999),
            jsonPath("$.traceId").isNotEmpty(),
            jsonPath("$.timestamp").exists());
  }

  @Test
  void mapsDuplicateResourceNumberToConflict() throws Exception {
    when(catalogApplicationService.createResource(any(CreateResourceCommand.class)))
        .thenThrow(
            new CatalogException(
                RESOURCE_NUMBER_ALREADY_EXISTS,
                "Resource number already exists",
                Map.of("resourceNo", "ROOM-A-101"),
                null));

    mockMvc
        .perform(
            post("/api/v1/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "resourceNo": "ROOM-A-101",
                          "categoryId": 10,
                          "name": "Room A101",
                          "capacity": 10
                        }
                        """))
        .andExpectAll(
            status().isConflict(),
            jsonPath("$.*", hasSize(5)),
            jsonPath("$.code").value("RESOURCE_NUMBER_ALREADY_EXISTS"),
            jsonPath("$.details.resourceNo").value("ROOM-A-101"),
            jsonPath("$.traceId").isNotEmpty(),
            jsonPath("$.timestamp").exists());
  }

  @Test
  void mapsUnsupportedStatusQueryToValidationError() throws Exception {
    mockMvc
        .perform(get("/api/v1/resources").queryParam("status", "RUNNING"))
        .andExpectAll(
            status().isBadRequest(),
            jsonPath("$.*", hasSize(5)),
            jsonPath("$.code").value("VALIDATION_ERROR"),
            jsonPath("$.message").value("Request parameter has an unsupported value"),
            jsonPath("$.details.parameter").value("status"),
            jsonPath("$.details.rejectedValue").value("RUNNING"));
  }

  @Test
  void mapsUnsupportedJsonStatusToValidationError() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/resources/{resourceId}/status", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "targetStatus": "RUNNING",
                          "expectedVersion": 1
                        }
                        """))
        .andExpectAll(
            status().isBadRequest(),
            jsonPath("$.*", hasSize(5)),
            jsonPath("$.code").value("VALIDATION_ERROR"),
            jsonPath("$.message")
                .value("Request body is malformed or contains an unsupported value"),
            jsonPath("$.details").isMap(),
            jsonPath("$.traceId").isNotEmpty(),
            jsonPath("$.timestamp").exists());
  }

  @Test
  void hidesPersistenceDetailsAndCredentials() throws Exception {
    CatalogException exception =
        new CatalogException(
            CATALOG_PERSISTENCE_ERROR,
            "Resource catalog operation failed",
            Map.of(
                "reason",
                "SELECT * FROM resource "
                    + "jdbc:mysql://localhost:13306/"
                    + "venueflow_resource "
                    + "username=venueflow_resource_app "
                    + "password=secret"),
            new IllegalStateException("SQLException caused by ResourceEntity"));

    when(catalogApplicationService.getResource(500L)).thenThrow(exception);

    mockMvc
        .perform(get("/api/v1/resources/{resourceId}", 500L))
        .andExpectAll(
            status().isInternalServerError(),
            jsonPath("$.*", hasSize(5)),
            jsonPath("$.code").value("CATALOG_PERSISTENCE_ERROR"),
            jsonPath("$.message").value("Resource catalog operation failed"),
            jsonPath("$.details").isEmpty(),
            jsonPath("$.traceId").isNotEmpty(),
            jsonPath("$.timestamp").exists(),
            content().string(not(containsString("SELECT"))),
            content().string(not(containsString("jdbc:mysql"))),
            content().string(not(containsString("password"))),
            content().string(not(containsString("SQLException"))),
            content().string(not(containsString("ResourceEntity"))));
  }

  private static ResourceResult resourceResult(ResourceStatus status, Long version) {
    return new ResourceResult(
        100L,
        "ROOM-A-101",
        10L,
        "Room A101",
        "Meeting room",
        "Building A",
        10,
        status,
        version,
        TIMESTAMP,
        TIMESTAMP);
  }

  @Test
  void appliesDefaultPaginationAtHttpBoundary() throws Exception {
    when(catalogApplicationService.listResources(any(ResourcePageQuery.class)))
        .thenReturn(new ResourcePageResult(List.of(), 0, 20, 0L, 0L));

    mockMvc
        .perform(get("/api/v1/resources"))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.page").value(0),
            jsonPath("$.size").value(20),
            jsonPath("$.items").isEmpty(),
            jsonPath("$.totalElements").value(0),
            jsonPath("$.totalPages").value(0));

    ArgumentCaptor<ResourcePageQuery> queryCaptor =
        ArgumentCaptor.forClass(ResourcePageQuery.class);

    verify(catalogApplicationService).listResources(queryCaptor.capture());

    ResourcePageQuery query = queryCaptor.getValue();

    assertThat(query.page()).isZero();
    assertThat(query.size()).isEqualTo(20);
    assertThat(query.offset()).isZero();
    assertThat(query.categoryId()).isNull();
    assertThat(query.status()).isNull();
  }

  @Test
  void acceptsDocumentedMaximumPageSize() throws Exception {
    when(catalogApplicationService.listResources(any(ResourcePageQuery.class)))
        .thenReturn(new ResourcePageResult(List.of(), 0, 100, 0L, 0L));

    mockMvc
        .perform(get("/api/v1/resources").queryParam("size", "100"))
        .andExpectAll(status().isOk(), jsonPath("$.page").value(0), jsonPath("$.size").value(100));

    ArgumentCaptor<ResourcePageQuery> queryCaptor =
        ArgumentCaptor.forClass(ResourcePageQuery.class);

    verify(catalogApplicationService).listResources(queryCaptor.capture());

    assertThat(queryCaptor.getValue().size()).isEqualTo(100);

    assertThat(queryCaptor.getValue().offset()).isZero();
  }

  @Test
  void rejectsOversizedPageBeforeCallingApplicationService() throws Exception {
    mockMvc
        .perform(get("/api/v1/resources").queryParam("size", "101"))
        .andExpectAll(
            status().isBadRequest(),
            jsonPath("$.code").value("VALIDATION_ERROR"),
            jsonPath("$.message").value("Request validation failed"),
            jsonPath("$.traceId").isNotEmpty(),
            jsonPath("$.timestamp").exists());

    verifyNoInteractions(catalogApplicationService);
  }

  @Test
  void forwardsCategoryAndStatusFiltersToApplicationQuery() throws Exception {
    when(catalogApplicationService.listResources(any(ResourcePageQuery.class)))
        .thenReturn(new ResourcePageResult(List.of(), 2, 25, 0L, 0L));

    mockMvc
        .perform(
            get("/api/v1/resources")
                .queryParam("page", "2")
                .queryParam("size", "25")
                .queryParam("categoryId", "10")
                .queryParam("status", "SUSPENDED"))
        .andExpectAll(status().isOk(), jsonPath("$.page").value(2), jsonPath("$.size").value(25));

    ArgumentCaptor<ResourcePageQuery> queryCaptor =
        ArgumentCaptor.forClass(ResourcePageQuery.class);

    verify(catalogApplicationService).listResources(queryCaptor.capture());

    ResourcePageQuery query = queryCaptor.getValue();

    assertThat(query.page()).isEqualTo(2);
    assertThat(query.size()).isEqualTo(25);
    assertThat(query.offset()).isEqualTo(50L);
    assertThat(query.categoryId()).isEqualTo(10L);
    assertThat(query.status()).isEqualTo(ResourceStatus.SUSPENDED);
  }
}
