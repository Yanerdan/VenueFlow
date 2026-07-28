package com.yanerdan.venueflow.booking.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yanerdan.venueflow.booking.application.BookingOperationalReport;
import com.yanerdan.venueflow.booking.application.BookingOperationalReportService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BookingOperationalReportController.class)
@ActiveProfiles("persistence")
class BookingOperationalReportControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private BookingOperationalReportService service;

  @Test
  void returnsGlobalReportForSystemAdmin() throws Exception {
    when(service.report("admin-id", "SYSTEM_ADMIN")).thenReturn(report());

    mockMvc
        .perform(
            get("/api/v1/bookings/management/report")
                .header("X-Role", "SYSTEM_ADMIN")
                .header("X-User-Id", "admin-id"))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.data.summary.totalBookings").value(6),
            jsonPath("$.data.summary.approvalRate").value(75.0),
            jsonPath("$.data.resources[0].resourceId").value(7),
            jsonPath("$.data.departments[0].department").value("校团委"));
    verify(service).report("admin-id", "SYSTEM_ADMIN");
  }

  @Test
  void rejectsApplicant() throws Exception {
    mockMvc
        .perform(get("/api/v1/bookings/management/report").header("X-Role", "APPLICANT"))
        .andExpectAll(status().isForbidden(), jsonPath("$.code").value("BOOKING_FORBIDDEN"));
  }

  private static BookingOperationalReport report() {
    return new BookingOperationalReport(
        new BookingOperationalReport.Summary(6, 2, 3, 1, 40, 75.0),
        List.of(new BookingOperationalReport.ResourceBreakdown(7L, 4, 30)),
        List.of(new BookingOperationalReport.DepartmentBreakdown("校团委", 4, 30)),
        List.of());
  }
}
