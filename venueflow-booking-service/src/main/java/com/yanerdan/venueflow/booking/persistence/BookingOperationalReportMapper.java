package com.yanerdan.venueflow.booking.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BookingOperationalReportMapper {
  @Select(
      """
      SELECT COUNT(*) AS totalBookings,
        COALESCE(SUM(status = 'PENDING_CONFIRMATION'), 0) AS pendingBookings,
        COALESCE(SUM(review_decision = 'APPROVED'), 0) AS approvedBookings,
        COALESCE(SUM(status = 'COMPLETED'), 0) AS completedBookings,
        COALESCE(SUM(quantity), 0) AS totalAttendees,
        COALESCE(SUM(review_decision IS NOT NULL), 0) AS reviewedBookings
      FROM booking_reservation
      WHERE (#{approverId} IS NULL
        OR assigned_approver_external_user_id = #{approverId})
      """)
  SummaryRow selectSummary(@Param("approverId") String approverId);

  @Select(
      """
      SELECT resource_id AS resourceId, COUNT(*) AS bookingCount,
        COALESCE(SUM(quantity), 0) AS attendeeCount
      FROM booking_reservation
      WHERE resource_id IS NOT NULL
        AND (#{approverId} IS NULL
          OR assigned_approver_external_user_id = #{approverId})
      GROUP BY resource_id
      ORDER BY bookingCount DESC, attendeeCount DESC, resource_id
      LIMIT 20
      """)
  List<ResourceRow> selectResources(@Param("approverId") String approverId);

  @Select(
      """
      SELECT COALESCE(NULLIF(TRIM(owner_department), ''), '未归属') AS department,
        COUNT(*) AS bookingCount, COALESCE(SUM(quantity), 0) AS attendeeCount
      FROM booking_reservation
      WHERE (#{approverId} IS NULL
        OR assigned_approver_external_user_id = #{approverId})
      GROUP BY COALESCE(NULLIF(TRIM(owner_department), ''), '未归属')
      ORDER BY bookingCount DESC, attendeeCount DESC, department
      LIMIT 20
      """)
  List<DepartmentRow> selectDepartments(@Param("approverId") String approverId);

  @Select(
      """
      SELECT booking_no AS bookingNo, review_decision AS decision,
        reviewer_role AS reviewerRole, review_note AS reviewNote, reviewed_at AS reviewedAt
      FROM booking_reservation
      WHERE reviewed_at IS NOT NULL
        AND (#{approverId} IS NULL
          OR assigned_approver_external_user_id = #{approverId})
      ORDER BY reviewed_at DESC, id DESC
      LIMIT 20
      """)
  List<ReviewRow> selectRecentReviews(@Param("approverId") String approverId);

  class SummaryRow {
    private long totalBookings;
    private long pendingBookings;
    private long approvedBookings;
    private long completedBookings;
    private long totalAttendees;
    private long reviewedBookings;

    public long getTotalBookings() {
      return totalBookings;
    }

    public void setTotalBookings(long value) {
      totalBookings = value;
    }

    public long getPendingBookings() {
      return pendingBookings;
    }

    public void setPendingBookings(long value) {
      pendingBookings = value;
    }

    public long getApprovedBookings() {
      return approvedBookings;
    }

    public void setApprovedBookings(long value) {
      approvedBookings = value;
    }

    public long getCompletedBookings() {
      return completedBookings;
    }

    public void setCompletedBookings(long value) {
      completedBookings = value;
    }

    public long getTotalAttendees() {
      return totalAttendees;
    }

    public void setTotalAttendees(long value) {
      totalAttendees = value;
    }

    public long getReviewedBookings() {
      return reviewedBookings;
    }

    public void setReviewedBookings(long value) {
      reviewedBookings = value;
    }
  }

  class ResourceRow {
    private Long resourceId;
    private long bookingCount;
    private long attendeeCount;

    public Long getResourceId() {
      return resourceId;
    }

    public void setResourceId(Long value) {
      resourceId = value;
    }

    public long getBookingCount() {
      return bookingCount;
    }

    public void setBookingCount(long value) {
      bookingCount = value;
    }

    public long getAttendeeCount() {
      return attendeeCount;
    }

    public void setAttendeeCount(long value) {
      attendeeCount = value;
    }
  }

  class DepartmentRow {
    private String department;
    private long bookingCount;
    private long attendeeCount;

    public String getDepartment() {
      return department;
    }

    public void setDepartment(String value) {
      department = value;
    }

    public long getBookingCount() {
      return bookingCount;
    }

    public void setBookingCount(long value) {
      bookingCount = value;
    }

    public long getAttendeeCount() {
      return attendeeCount;
    }

    public void setAttendeeCount(long value) {
      attendeeCount = value;
    }
  }

  class ReviewRow {
    private String bookingNo;
    private String decision;
    private String reviewerRole;
    private String reviewNote;
    private LocalDateTime reviewedAt;

    public String getBookingNo() {
      return bookingNo;
    }

    public void setBookingNo(String value) {
      bookingNo = value;
    }

    public String getDecision() {
      return decision;
    }

    public void setDecision(String value) {
      decision = value;
    }

    public String getReviewerRole() {
      return reviewerRole;
    }

    public void setReviewerRole(String value) {
      reviewerRole = value;
    }

    public String getReviewNote() {
      return reviewNote;
    }

    public void setReviewNote(String value) {
      reviewNote = value;
    }

    public LocalDateTime getReviewedAt() {
      return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime value) {
      reviewedAt = value;
    }
  }
}
