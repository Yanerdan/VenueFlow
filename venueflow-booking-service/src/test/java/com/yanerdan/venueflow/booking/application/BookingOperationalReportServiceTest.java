package com.yanerdan.venueflow.booking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanerdan.venueflow.booking.persistence.BookingOperationalReportMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingOperationalReportServiceTest {
  @Mock private BookingOperationalReportMapper mapper;

  @Test
  void returnsGlobalSummaryWithApprovalRate() {
    BookingOperationalReportMapper.SummaryRow summary = summary(8, 2, 3, 1, 50, 4);
    when(mapper.selectSummary(null)).thenReturn(summary);
    when(mapper.selectResources(null)).thenReturn(List.of());
    when(mapper.selectDepartments(null)).thenReturn(List.of());
    when(mapper.selectRecentReviews(null)).thenReturn(List.of());

    BookingOperationalReport report =
        new BookingOperationalReportService(mapper).report("admin-id", "SYSTEM_ADMIN");

    assertThat(report.summary().totalBookings()).isEqualTo(8);
    assertThat(report.summary().approvalRate()).isEqualTo(75.0);
    verify(mapper).selectSummary(null);
  }

  @Test
  void scopesApproverReportToTrustedIdentity() {
    when(mapper.selectSummary("approver-id")).thenReturn(summary(0, 0, 0, 0, 0, 0));
    when(mapper.selectResources("approver-id")).thenReturn(List.of());
    when(mapper.selectDepartments("approver-id")).thenReturn(List.of());
    when(mapper.selectRecentReviews("approver-id")).thenReturn(List.of());

    new BookingOperationalReportService(mapper).report("approver-id", "APPROVER");

    verify(mapper).selectSummary("approver-id");
  }

  @Test
  void rejectsApproverWithoutTrustedIdentity() {
    assertThatThrownBy(
            () -> new BookingOperationalReportService(mapper).report(null, "APPROVER"))
        .isInstanceOf(BookingException.class)
        .extracting("code")
        .isEqualTo(BookingErrorCode.BOOKING_FORBIDDEN);
  }

  private static BookingOperationalReportMapper.SummaryRow summary(
      long total, long pending, long approved, long completed, long attendees, long reviewed) {
    BookingOperationalReportMapper.SummaryRow row =
        new BookingOperationalReportMapper.SummaryRow();
    row.setTotalBookings(total);
    row.setPendingBookings(pending);
    row.setApprovedBookings(approved);
    row.setCompletedBookings(completed);
    row.setTotalAttendees(attendees);
    row.setReviewedBookings(reviewed);
    return row;
  }
}
