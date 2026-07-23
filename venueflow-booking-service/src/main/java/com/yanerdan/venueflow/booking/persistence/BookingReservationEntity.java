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
  private int quantity;
  private BookingStatus status;
  private String allocationOperationId;
  private String releaseOperationId;
  private long version;
  private LocalDateTime createdAt;
  private LocalDateTime confirmedAt;
  private LocalDateTime cancelledAt;
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
        confirmedAt,
        cancelledAt,
        updatedAt);
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

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

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

  public void setConfirmedAt(LocalDateTime confirmedAt) {
    this.confirmedAt = confirmedAt;
  }

  public LocalDateTime getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(LocalDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
