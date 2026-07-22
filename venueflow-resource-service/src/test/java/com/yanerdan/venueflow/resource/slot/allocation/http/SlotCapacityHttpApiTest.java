package com.yanerdan.venueflow.resource.slot.allocation.http;

import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.INSUFFICIENT_SLOT_CAPACITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import com.yanerdan.venueflow.resource.slot.allocation.application.SlotCapacityApplicationService;
import com.yanerdan.venueflow.resource.slot.allocation.application.SlotCapacityChangeCommand;
import com.yanerdan.venueflow.resource.slot.allocation.application.SlotCapacityChangeResult;
import com.yanerdan.venueflow.resource.slot.allocation.application.SlotCapacityResult;
import com.yanerdan.venueflow.resource.slot.allocation.domain.SlotAllocationOperationType;
import com.yanerdan.venueflow.resource.slot.allocation.http.controller.SlotCapacityController;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SlotCapacityController.class)
@ActiveProfiles("persistence")
class SlotCapacityHttpApiTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SlotCapacityApplicationService slotCapacityApplicationService;

  @Test
  void allocatesThroughDtoBoundaryAndReturnsCapacityFacts() throws Exception {
    when(slotCapacityApplicationService.allocate(any(SlotCapacityChangeCommand.class)))
        .thenReturn(changeResult("op-1", SlotAllocationOperationType.ALLOCATE, 2, 5));

    mockMvc
        .perform(
            post("/api/v1/resource-slots/{slotId}/allocations", 500L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"operationId\": \"op-1\", \"quantity\": 2 }"))
        .andExpectAll(
            status().isCreated(),
            jsonPath("$.*", hasSize(4)),
            jsonPath("$.operationId").value("op-1"),
            jsonPath("$.operationType").value("ALLOCATE"),
            jsonPath("$.capacity.occupiedQuantity").value(5),
            jsonPath("$.capacity.availableQuantity").value(5));

    ArgumentCaptor<SlotCapacityChangeCommand> captor =
        ArgumentCaptor.forClass(SlotCapacityChangeCommand.class);
    verify(slotCapacityApplicationService).allocate(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new SlotCapacityChangeCommand(500L, "op-1", 2));
  }

  @Test
  void exposesCapacityAndRejectsInvalidRequestsBeforeService() throws Exception {
    when(slotCapacityApplicationService.getCapacity(500L))
        .thenReturn(new SlotCapacityResult(500L, 10, 3, 7, ResourceSlotStatus.OPEN));

    mockMvc
        .perform(get("/api/v1/resource-slots/{slotId}/capacity", 500L))
        .andExpectAll(status().isOk(), jsonPath("$.availableQuantity").value(7));

    mockMvc
        .perform(
            post("/api/v1/resource-slots/{slotId}/releases", 500L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"operationId\": \"\", \"quantity\": 0 }"))
        .andExpectAll(status().isBadRequest(), jsonPath("$.code").value("VALIDATION_ERROR"));

    verify(slotCapacityApplicationService).getCapacity(500L);
    verifyNoMoreInteractions(slotCapacityApplicationService);
  }

  @Test
  void returnsSafeCapacityConflictEnvelope() throws Exception {
    when(slotCapacityApplicationService.allocate(any(SlotCapacityChangeCommand.class)))
        .thenThrow(
            new CatalogException(
                INSUFFICIENT_SLOT_CAPACITY,
                "Requested quantity exceeds available slot capacity",
                java.util.Map.of("slotId", 500L),
                null));

    mockMvc
        .perform(
            post("/api/v1/resource-slots/{slotId}/allocations", 500L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"operationId\": \"op-2\", \"quantity\": 10 }"))
        .andExpectAll(
            status().isConflict(),
            jsonPath("$.*", hasSize(5)),
            jsonPath("$.code").value("INSUFFICIENT_SLOT_CAPACITY"),
            jsonPath("$.details.slotId").value(500),
            jsonPath("$.traceId").isNotEmpty());
  }

  private static SlotCapacityChangeResult changeResult(
      String operationId, SlotAllocationOperationType type, int quantity, int occupied) {
    return new SlotCapacityChangeResult(
        operationId,
        type,
        quantity,
        new SlotCapacityResult(500L, 10, occupied, 10 - occupied, ResourceSlotStatus.OPEN));
  }
}
