package com.yanerdan.venueflow.booking.application;

import java.time.LocalDateTime;
import java.util.List;

public record BookingOperationalReport(
    Summary summary,
    List<ResourceBreakdown> resources,
    List<DepartmentBreakdown> departments,
    List<ReviewAudit> recentReviews) {

  public BookingOperationalReport {
    resources = List.copyOf(resources);
    departments = List.copyOf(departments);
    recentReviews = List.copyOf(recentReviews);
  }

  public record Summary(
      long totalBookings,
      long pendingBookings,
      long approvedBookings,
      long completedBookings,
      long totalAttendees,
      double approvalRate) {}

  public record ResourceBreakdown(Long resourceId, long bookingCount, long attendeeCount) {}

  public record DepartmentBreakdown(
      String department, long bookingCount, long attendeeCount) {}

  public record ReviewAudit(
      String bookingNo,
      String decision,
      String reviewerRole,
      String reviewNote,
      LocalDateTime reviewedAt) {}
}
