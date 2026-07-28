package com.yanerdan.venueflow.booking.web;

import com.yanerdan.venueflow.booking.application.BookingOperationalReport;
import com.yanerdan.venueflow.booking.application.BookingOperationalReportService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("persistence")
@RequestMapping("/api/v1/bookings/management/report")
public class BookingOperationalReportController {
  private final BookingOperationalReportService service;

  public BookingOperationalReportController(BookingOperationalReportService service) {
    this.service = service;
  }

  @GetMapping
  public BookingReservationController.SuccessEnvelope<BookingOperationalReport> report(
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role,
      @RequestHeader(value = "X-User-Id", required = false) String trustedUserId) {
    BookingRoleGuard.requireApprover(role);
    return BookingReservationController.SuccessEnvelope.of(service.report(trustedUserId, role));
  }
}
