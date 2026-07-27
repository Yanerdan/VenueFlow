package com.yanerdan.venueflow.notification.inbox;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationInboxController.class)
@ActiveProfiles("persistence")
class NotificationInboxControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private NotificationInboxService service;

  @Test
  void returnsBoundedUserInbox() throws Exception {
    NotificationInboxItem item =
        new NotificationInboxItem(
            9L,
            42L,
            "booking-1",
            "BOOKING_CONFIRMED",
            "Booking confirmed",
            "Your booking is confirmed.",
            Instant.parse("2026-07-27T01:00:00Z"));
    when(service.findByUser(42L, 0, 20))
        .thenReturn(new NotificationInboxPage(List.of(item), 1L, 0, 20));

    mockMvc
        .perform(
            get("/api/v1/notifications")
                .param("userId", "42")
                .header("X-Trace-Id", "861b4f56-f37d-451c-a5a4-41de4c5b6392"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items", hasSize(1)))
        .andExpect(jsonPath("$.data.items[0].bookingNo").value("booking-1"))
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.traceId").value("861b4f56-f37d-451c-a5a4-41de4c5b6392"));
  }
}
