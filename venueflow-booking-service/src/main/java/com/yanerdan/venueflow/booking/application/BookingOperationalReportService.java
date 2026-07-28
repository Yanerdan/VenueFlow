package com.yanerdan.venueflow.booking.application;

import com.yanerdan.venueflow.booking.persistence.BookingOperationalReportMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("persistence")
public class BookingOperationalReportService {
  private final BookingOperationalReportMapper mapper;

  public BookingOperationalReportService(BookingOperationalReportMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public BookingOperationalReport report(String trustedUserId, String role) {
    String approverId = scope(trustedUserId, role);
    BookingOperationalReportMapper.SummaryRow row = mapper.selectSummary(approverId);
    long reviewed = row.getReviewedBookings();
    double approvalRate =
        reviewed == 0 ? 0 : Math.round(row.getApprovedBookings() * 1000.0 / reviewed) / 10.0;
    return new BookingOperationalReport(
        new BookingOperationalReport.Summary(
            row.getTotalBookings(),
            row.getPendingBookings(),
            row.getApprovedBookings(),
            row.getCompletedBookings(),
            row.getTotalAttendees(),
            approvalRate),
        mapper.selectResources(approverId).stream()
            .map(
                value ->
                    new BookingOperationalReport.ResourceBreakdown(
                        value.getResourceId(), value.getBookingCount(), value.getAttendeeCount()))
            .toList(),
        mapper.selectDepartments(approverId).stream()
            .map(
                value ->
                    new BookingOperationalReport.DepartmentBreakdown(
                        value.getDepartment(), value.getBookingCount(), value.getAttendeeCount()))
            .toList(),
        mapper.selectRecentReviews(approverId).stream()
            .map(
                value ->
                    new BookingOperationalReport.ReviewAudit(
                        value.getBookingNo(),
                        value.getDecision(),
                        value.getReviewerRole(),
                        value.getReviewNote(),
                        value.getReviewedAt()))
            .toList());
  }

  private static String scope(String trustedUserId, String role) {
    if ("SYSTEM_ADMIN".equals(role)) return null;
    if (!"APPROVER".equals(role) || trustedUserId == null || trustedUserId.isBlank()) {
      throw new BookingException(
          BookingErrorCode.BOOKING_FORBIDDEN, "Trusted approval identity is required");
    }
    return trustedUserId;
  }
}
