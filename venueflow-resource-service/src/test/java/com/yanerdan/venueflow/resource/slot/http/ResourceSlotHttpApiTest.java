package com.yanerdan.venueflow.resource.slot.http;

import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_SLOT_TIME_OVERLAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import com.yanerdan.venueflow.resource.slot.application.ChangeResourceSlotStatusCommand;
import com.yanerdan.venueflow.resource.slot.application.CreateResourceSlotCommand;
import com.yanerdan.venueflow.resource.slot.application.ResourceSlotApplicationService;
import com.yanerdan.venueflow.resource.slot.application.ResourceSlotPageQuery;
import com.yanerdan.venueflow.resource.slot.application.ResourceSlotPageResult;
import com.yanerdan.venueflow.resource.slot.application.ResourceSlotResult;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;
import com.yanerdan.venueflow.resource.slot.http.controller.ResourceSlotController;
import java.time.Instant;
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

@WebMvcTest(controllers = ResourceSlotController.class)
@ActiveProfiles("persistence")
class ResourceSlotHttpApiTest {

  private static final Instant START = Instant.parse("2026-07-23T10:00:00Z");
  private static final Instant END = Instant.parse("2026-07-23T11:00:00Z");
  private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 7, 22, 15, 0);

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ResourceSlotApplicationService resourceSlotApplicationService;

  @Test
  void createsSlotThroughDtoBoundaryAndNormalizesOffsetTime() throws Exception {
    when(resourceSlotApplicationService.createSlot(any(CreateResourceSlotCommand.class)))
        .thenReturn(slotResult(ResourceSlotStatus.OPEN, 1L));

    mockMvc
        .perform(
            post("/api/v1/resources/{resourceId}/slots", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                          "startAt": "2026-07-23T18:00:00+08:00",
                          "endAt": "2026-07-23T19:00:00+08:00"
                        }
                        """))
        .andExpectAll(
            status().isCreated(),
            jsonPath("$.*", hasSize(17)),
            jsonPath("$.id").value(500),
            jsonPath("$.resourceId").value(100),
            jsonPath("$.minAdvanceHours").value(0),
            jsonPath("$.maxAdvanceDays").value(90),
            jsonPath("$.maxDurationMinutes").value(480),
            jsonPath("$.startAt").value("2026-07-23T10:00:00Z"),
            jsonPath("$.endAt").value("2026-07-23T11:00:00Z"),
            jsonPath("$.status").value("OPEN"),
            jsonPath("$.version").value(1),
            jsonPath("$.entity").doesNotExist());

    ArgumentCaptor<CreateResourceSlotCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateResourceSlotCommand.class);
    verify(resourceSlotApplicationService).createSlot(commandCaptor.capture());
    assertThat(commandCaptor.getValue().resourceId()).isEqualTo(100L);
    assertThat(commandCaptor.getValue().startAt()).isEqualTo(START);
    assertThat(commandCaptor.getValue().endAt()).isEqualTo(END);
  }

  @Test
  void listsSlotsWithRequiredWindowAndDefaultPage() throws Exception {
    when(resourceSlotApplicationService.listSlots(any(ResourceSlotPageQuery.class)))
        .thenReturn(
            new ResourceSlotPageResult(
                List.of(slotResult(ResourceSlotStatus.OPEN, 1L)), 0, 20, 1, 1));

    mockMvc
        .perform(
            get("/api/v1/resources/{resourceId}/slots", 100L)
                .queryParam("from", "2026-07-23T10:00:00Z")
                .queryParam("to", "2026-07-23T12:00:00Z"))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.items", hasSize(1)),
            jsonPath("$.items[0].id").value(500),
            jsonPath("$.page").value(0),
            jsonPath("$.size").value(20),
            jsonPath("$.totalElements").value(1));

    ArgumentCaptor<ResourceSlotPageQuery> queryCaptor =
        ArgumentCaptor.forClass(ResourceSlotPageQuery.class);
    verify(resourceSlotApplicationService).listSlots(queryCaptor.capture());
    assertThat(queryCaptor.getValue().resourceId()).isEqualTo(100L);
    assertThat(queryCaptor.getValue().from()).isEqualTo(START);
    assertThat(queryCaptor.getValue().to()).isEqualTo(Instant.parse("2026-07-23T12:00:00Z"));
    assertThat(queryCaptor.getValue().size()).isEqualTo(20);
  }

  @Test
  void rejectsUnboundedAndOversizedSlotListsBeforeApplicationService() throws Exception {
    mockMvc
        .perform(get("/api/v1/resources/{resourceId}/slots", 100L))
        .andExpectAll(status().isBadRequest(), jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            get("/api/v1/resources/{resourceId}/slots", 100L)
                .queryParam("from", "2026-07-23T10:00:00Z")
                .queryParam("to", "2026-07-23T12:00:00Z")
                .queryParam("size", "101"))
        .andExpectAll(status().isBadRequest(), jsonPath("$.code").value("VALIDATION_ERROR"));

    verifyNoInteractions(resourceSlotApplicationService);
  }

  @Test
  void getsAndChangesSlotStatusThroughDtoBoundary() throws Exception {
    when(resourceSlotApplicationService.getSlot(500L))
        .thenReturn(slotResult(ResourceSlotStatus.OPEN, 1L));
    when(resourceSlotApplicationService.changeSlotStatus(
            any(ChangeResourceSlotStatusCommand.class)))
        .thenReturn(slotResult(ResourceSlotStatus.CLOSED, 2L));

    mockMvc
        .perform(get("/api/v1/resource-slots/{slotId}", 500L))
        .andExpectAll(status().isOk(), jsonPath("$.status").value("OPEN"));

    mockMvc
        .perform(
            patch("/api/v1/resource-slots/{slotId}/status", 500L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"targetStatus\": \"CLOSED\", \"expectedVersion\": 1 }"))
        .andExpectAll(
            status().isOk(), jsonPath("$.status").value("CLOSED"), jsonPath("$.version").value(2));

    ArgumentCaptor<ChangeResourceSlotStatusCommand> commandCaptor =
        ArgumentCaptor.forClass(ChangeResourceSlotStatusCommand.class);
    verify(resourceSlotApplicationService).changeSlotStatus(commandCaptor.capture());
    assertThat(commandCaptor.getValue())
        .isEqualTo(new ChangeResourceSlotStatusCommand(500L, ResourceSlotStatus.CLOSED, 1L));
  }

  @Test
  void exposesSlotConflictThroughSafeErrorEnvelope() throws Exception {
    when(resourceSlotApplicationService.createSlot(any(CreateResourceSlotCommand.class)))
        .thenThrow(
            new CatalogException(
                RESOURCE_SLOT_TIME_OVERLAP,
                "Resource Slot overlaps an existing slot",
                Map.of("resourceId", 100L),
                null));

    mockMvc
        .perform(
            post("/api/v1/resources/{resourceId}/slots", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        { "startAt": "2026-07-23T10:00:00Z", "endAt": "2026-07-23T11:00:00Z" }
                        """))
        .andExpectAll(
            status().isConflict(),
            jsonPath("$.*", hasSize(5)),
            jsonPath("$.code").value("RESOURCE_SLOT_TIME_OVERLAP"),
            jsonPath("$.details.resourceId").value(100),
            jsonPath("$.traceId").isNotEmpty(),
            jsonPath("$.timestamp").exists());
  }

  private static ResourceSlotResult slotResult(ResourceSlotStatus status, Long version) {
    return new ResourceSlotResult(500L, 100L, START, END, status, version, TIMESTAMP, TIMESTAMP);
  }
}
