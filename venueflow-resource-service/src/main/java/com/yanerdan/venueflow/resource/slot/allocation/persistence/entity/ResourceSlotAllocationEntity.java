package com.yanerdan.venueflow.resource.slot.allocation.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yanerdan.venueflow.resource.slot.allocation.domain.SlotAllocationOperationType;
import java.time.LocalDateTime;

@TableName("resource_slot_allocation")
public class ResourceSlotAllocationEntity {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("slot_id")
  private Long slotId;

  @TableField("operation_id")
  private String operationId;

  @TableField("operation_type")
  private SlotAllocationOperationType operationType;

  @TableField("quantity")
  private Integer quantity;

  @TableField("request_fingerprint")
  private String requestFingerprint;

  @TableField("occupied_quantity_after")
  private Integer occupiedQuantityAfter;

  @TableField("created_at")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getSlotId() {
    return slotId;
  }

  public void setSlotId(Long slotId) {
    this.slotId = slotId;
  }

  public String getOperationId() {
    return operationId;
  }

  public void setOperationId(String operationId) {
    this.operationId = operationId;
  }

  public SlotAllocationOperationType getOperationType() {
    return operationType;
  }

  public void setOperationType(SlotAllocationOperationType operationType) {
    this.operationType = operationType;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public String getRequestFingerprint() {
    return requestFingerprint;
  }

  public void setRequestFingerprint(String requestFingerprint) {
    this.requestFingerprint = requestFingerprint;
  }

  public Integer getOccupiedQuantityAfter() {
    return occupiedQuantityAfter;
  }

  public void setOccupiedQuantityAfter(Integer occupiedQuantityAfter) {
    this.occupiedQuantityAfter = occupiedQuantityAfter;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
