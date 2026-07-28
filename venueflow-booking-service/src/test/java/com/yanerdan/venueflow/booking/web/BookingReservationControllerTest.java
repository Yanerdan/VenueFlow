package com.yanerdan.venueflow.booking.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yanerdan.venueflow.booking.application.BookingReservationService;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import com.yanerdan.venueflow.booking.persistence.BookingRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BookingReservationController.class)
@ActiveProfiles("persistence")
class BookingReservationControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private BookingReservationService service;

  @Test
  void createsThroughHeaderAndDtoEnvelope() throws Exception {
    when(service.create(
            anyString(),
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(new BookingReservationService.CreateResult(reservation(), false));

    mockMvc
        .perform(
            post("/api/v1/bookings")
                .header("Idempotency-Key", "f4f4266a-b145-44f4-a375-0d59450f5147")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"slotId\":2,\"quantity\":1}"))
        .andExpectAll(
            status().isCreated(),
            jsonPath("$.*", hasSize(4)),
            jsonPath("$.code").value("OK"),
            jsonPath("$.data.bookingNo").value("booking-1"),
            jsonPath("$.data.status").value("CONFIRMED"));
  }

  @Test
  void rejectsMissingHeaderWithSafeEnvelope() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"slotId\":2,\"quantity\":1}"))
        .andExpectAll(
            status().isBadRequest(),
            jsonPath("$.*", hasSize(5)),
            jsonPath("$.code").value("BOOKING_VALIDATION_FAILED"));
  }

  @Test
  void checksInThroughBoundedDtoEndpoint() throws Exception {
    when(service.checkIn("booking-1")).thenReturn(reservation(BookingStatus.COMPLETED));

    mockMvc
        .perform(
            post("/api/v1/bookings/{bookingNo}/check-in", "booking-1")
                .header("X-Role", "SYSTEM_ADMIN"))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.data.bookingNo").value("booking-1"),
            jsonPath("$.data.status").value("COMPLETED"));
  }

  @Test
  void rejectsApplicantManagementAction() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/bookings/{bookingNo}/confirmation", "booking-1")
                .header("X-Role", "APPLICANT"))
        .andExpectAll(status().isForbidden(), jsonPath("$.code").value("BOOKING_FORBIDDEN"));
  }

  @Test
  void listsBoundedManagementHistory() throws Exception {
    when(service.managementHistory(BookingStatus.PENDING_CONFIRMATION, 0, 20))
        .thenReturn(
            new BookingRepository.BookingHistoryPage(
                List.of(reservation(BookingStatus.PENDING_CONFIRMATION)), 1L, 0, 20));

    mockMvc
        .perform(
            get("/api/v1/bookings/management")
                .header("X-Role", "APPROVER")
                .param("status", "PENDING_CONFIRMATION"))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.data.items", hasSize(1)),
            jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void listsBoundedBookingHistory() throws Exception {
    when(service.history(1L, 0, 20))
        .thenReturn(new BookingRepository.BookingHistoryPage(List.of(reservation()), 1L, 0, 20));

    mockMvc
        .perform(get("/api/v1/bookings").param("userId", "1"))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.data.items", hasSize(1)),
            jsonPath("$.data.items[0].bookingNo").value("booking-1"),
            jsonPath("$.data.totalElements").value(1),
            jsonPath("$.data.pageNumber").value(0),
            jsonPath("$.data.pageSize").value(20));
  }

  @Test
  void rejectsOversizedHistoryPage() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/bookings")
                .param("userId", "1")
                .param("pageNumber", "0")
                .param("pageSize", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BOOKING_VALIDATION_FAILED"));
  }

  private static BookingReservation reservation() {
    return reservation(BookingStatus.CONFIRMED);
  }

  private static BookingReservation reservation(BookingStatus status) {
    LocalDateTime now = LocalDateTime.of(2026, 7, 23, 12, 0);
    return new BookingReservation(
        1L,
        "booking-1",
        "request-1",
        1L,
        2L,
        1,
        status,
        "allocate:request-1",
        "release:request-1",
        0L,
        now,
        now,
        null,
        now);
  }
}
