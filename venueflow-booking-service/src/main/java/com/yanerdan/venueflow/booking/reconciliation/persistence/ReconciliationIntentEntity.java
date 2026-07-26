package com.yanerdan.venueflow.booking.reconciliation.persistence;

import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntentState;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationWorkflowType;
import java.time.LocalDateTime;

public class ReconciliationIntentEntity {
  private long id;
  private ReconciliationWorkflowType workflowType;
  private String requestId;
  private Long bookingId;
  private long slotId;
  private int quantity;
  private String allocationOperationId;
  private String releaseOperationId;
  private ReconciliationIntentState state;
  private int attemptCount;
  private long version;
  private String leaseOwner;
  private LocalDateTime nextCheckAt;
  private LocalDateTime createdAt;

  public ReconciliationIntent toDomain() {
    return new ReconciliationIntent(
        id,
        workflowType,
        requestId,
        bookingId,
        slotId,
        quantity,
        allocationOperationId,
        releaseOperationId,
        state,
        attemptCount,
        version,
        leaseOwner,
        nextCheckAt,
        createdAt);
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public ReconciliationWorkflowType getWorkflowType() {
    return workflowType;
  }

  public void setWorkflowType(ReconciliationWorkflowType workflowType) {
    this.workflowType = workflowType;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public Long getBookingId() {
    return bookingId;
  }

  public void setBookingId(Long bookingId) {
    this.bookingId = bookingId;
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

  public String getAllocationOperationId() {
    return allocationOperationId;
  }

  public void setAllocationOperationId(String allocationOperationId) {
    this.allocationOperationId = allocationOperationId;
  }

  public String getReleaseOperationId() {
    return releaseOperationId;
  }

  public void setReleaseOperationId(String releaseOperationId) {
    this.releaseOperationId = releaseOperationId;
  }

  public ReconciliationIntentState getState() {
    return state;
  }

  public void setState(ReconciliationIntentState state) {
    this.state = state;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(int attemptCount) {
    this.attemptCount = attemptCount;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public String getLeaseOwner() {
    return leaseOwner;
  }

  public void setLeaseOwner(String leaseOwner) {
    this.leaseOwner = leaseOwner;
  }

  public LocalDateTime getNextCheckAt() {
    return nextCheckAt;
  }

  public void setNextCheckAt(LocalDateTime nextCheckAt) {
    this.nextCheckAt = nextCheckAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
