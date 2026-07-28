package com.yanerdan.venueflow.booking.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import java.time.LocalDateTime;

@TableName("booking_reservation")
public class BookingReservationEntity {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String bookingNo;
  private String requestId;
  private long userId;
  private long slotId;
  private Long resourceId;
  private String ownerDepartment;
  private String assignedApproverExternalUserId;
  private int quantity;
  private String activityTitle;
  private String applicationPurpose;
  private String contactName;
  private String contactPhone;
  private String applicationNote;
  private BookingStatus status;
  private String allocationOperationId;
  private String releaseOperationId;
  private long version;
  private LocalDateTime createdAt;
  private LocalDateTime expireAt;
  private LocalDateTime confirmedAt;
  private LocalDateTime cancelledAt;
  private LocalDateTime expiredAt;
  private LocalDateTime completedAt;
  private String terminalReason;
  private String reviewDecision;
  private String reviewNote;
  private String reviewerRole;
  private LocalDateTime reviewedAt;
  private String timeoutState;
  private String timeoutLeaseOwner;
  private LocalDateTime timeoutLeaseExpiresAt;
  private int timeoutAttemptCount;
  private LocalDateTime timeoutNextCheckAt;
  private String timeoutLastErrorCode;
  private LocalDateTime updatedAt;

  public BookingReservation toDomain() {
    return new BookingReservation(
        id,
        bookingNo,
        requestId,
        userId,
        slotId,
        quantity,
        status,
        allocationOperationId,
        releaseOperationId,
        version,
        createdAt,
        expireAt,
        confirmedAt,
        cancelledAt,
        expiredAt,
        completedAt,
        terminalReason,
        updatedAt,
        activityTitle,
        applicationPurpose,
        contactName,
        contactPhone,
        applicationNote,
        reviewDecision,
        reviewNote,
        reviewerRole,
        reviewedAt,
        resourceId,
        ownerDepartment,
        assignedApproverExternalUserId);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getBookingNo() {
    return bookingNo;
  }

  public void setBookingNo(String bookingNo) {
    this.bookingNo = bookingNo;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  public long getSlotId() {
    return slotId;
  }

  public void setSlotId(long slotId) {
    this.slotId = slotId;
  }

  public Long getResourceId() { return resourceId; }

  public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

  public String getOwnerDepartment() { return ownerDepartment; }

  public void setOwnerDepartment(String ownerDepartment) { this.ownerDepartment = ownerDepartment; }

  public String getAssignedApproverExternalUserId() { return assignedApproverExternalUserId; }

  public void setAssignedApproverExternalUserId(String value) {
    this.assignedApproverExternalUserId = value;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public String getActivityTitle() { return activityTitle; }

  public void setActivityTitle(String activityTitle) { this.activityTitle = activityTitle; }

  public String getApplicationPurpose() { return applicationPurpose; }

  public void setApplicationPurpose(String applicationPurpose) {
    this.applicationPurpose = applicationPurpose;
  }

  public String getContactName() { return contactName; }

  public void setContactName(String contactName) { this.contactName = contactName; }

  public String getContactPhone() { return contactPhone; }

  public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

  public String getApplicationNote() { return applicationNote; }

  public void setApplicationNote(String applicationNote) { this.applicationNote = applicationNote; }

  public BookingStatus getStatus() {
    return status;
  }

  public void setStatus(BookingStatus status) {
    this.status = status;
  }

  public String getAllocationOperationId() {
    return allocationOperationId;
  }

  public void setAllocationOperationId(String value) {
    this.allocationOperationId = value;
  }

  public String getReleaseOperationId() {
    return releaseOperationId;
  }

  public void setReleaseOperationId(String value) {
    this.releaseOperationId = value;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getConfirmedAt() {
    return confirmedAt;
  }

  public LocalDateTime getExpireAt() {
    return expireAt;
  }

  public void setExpireAt(LocalDateTime expireAt) {
    this.expireAt = expireAt;
  }

  public void setConfirmedAt(LocalDateTime confirmedAt) {
    this.confirmedAt = confirmedAt;
  }

  public LocalDateTime getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(LocalDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
  }

  public LocalDateTime getExpiredAt() {
    return expiredAt;
  }

  public void setExpiredAt(LocalDateTime expiredAt) {
    this.expiredAt = expiredAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public String getTerminalReason() {
    return terminalReason;
  }

  public void setTerminalReason(String terminalReason) {
    this.terminalReason = terminalReason;
  }

  public String getReviewDecision() { return reviewDecision; }

  public void setReviewDecision(String reviewDecision) { this.reviewDecision = reviewDecision; }

  public String getReviewNote() { return reviewNote; }

  public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

  public String getReviewerRole() { return reviewerRole; }

  public void setReviewerRole(String reviewerRole) { this.reviewerRole = reviewerRole; }

  public LocalDateTime getReviewedAt() { return reviewedAt; }

  public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

  public String getTimeoutState() {
    return timeoutState;
  }

  public void setTimeoutState(String timeoutState) {
    this.timeoutState = timeoutState;
  }

  public String getTimeoutLeaseOwner() {
    return timeoutLeaseOwner;
  }

  public void setTimeoutLeaseOwner(String timeoutLeaseOwner) {
    this.timeoutLeaseOwner = timeoutLeaseOwner;
  }

  public LocalDateTime getTimeoutLeaseExpiresAt() {
    return timeoutLeaseExpiresAt;
  }

  public void setTimeoutLeaseExpiresAt(LocalDateTime timeoutLeaseExpiresAt) {
    this.timeoutLeaseExpiresAt = timeoutLeaseExpiresAt;
  }

  public int getTimeoutAttemptCount() {
    return timeoutAttemptCount;
  }

  public void setTimeoutAttemptCount(int timeoutAttemptCount) {
    this.timeoutAttemptCount = timeoutAttemptCount;
  }

  public LocalDateTime getTimeoutNextCheckAt() {
    return timeoutNextCheckAt;
  }

  public void setTimeoutNextCheckAt(LocalDateTime timeoutNextCheckAt) {
    this.timeoutNextCheckAt = timeoutNextCheckAt;
  }

  public String getTimeoutLastErrorCode() {
    return timeoutLastErrorCode;
  }

  public void setTimeoutLastErrorCode(String timeoutLastErrorCode) {
    this.timeoutLastErrorCode = timeoutLastErrorCode;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
