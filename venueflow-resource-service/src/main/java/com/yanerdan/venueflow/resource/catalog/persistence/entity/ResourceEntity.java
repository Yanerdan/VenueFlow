package com.yanerdan.venueflow.resource.catalog.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import com.yanerdan.venueflow.resource.catalog.domain.ApprovalMode;
import java.time.LocalDateTime;

@TableName("resource")
public class ResourceEntity {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("resource_no")
  private String resourceNo;

  @TableField("category_id")
  private Long categoryId;

  @TableField("name")
  private String name;

  @TableField("description")
  private String description;

  @TableField("location")
  private String location;

  @TableField("capacity")
  private Integer capacity;

  @TableField("owner_department")
  private String ownerDepartment;

  @TableField("approver_external_user_id")
  private String approverExternalUserId;

  @TableField("approval_mode")
  private ApprovalMode approvalMode;

  @TableField("final_approver_external_user_id")
  private String finalApproverExternalUserId;

  @TableField("status")
  private ResourceStatus status;

  @TableField("version")
  private Long version;

  @TableField("created_at")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  private LocalDateTime updatedAt;

  public ResourceEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getResourceNo() {
    return resourceNo;
  }

  public void setResourceNo(String resourceNo) {
    this.resourceNo = resourceNo;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public String getOwnerDepartment() {
    return ownerDepartment;
  }

  public void setOwnerDepartment(String ownerDepartment) {
    this.ownerDepartment = ownerDepartment;
  }

  public String getApproverExternalUserId() {
    return approverExternalUserId;
  }

  public void setApproverExternalUserId(String approverExternalUserId) {
    this.approverExternalUserId = approverExternalUserId;
  }

  public ApprovalMode getApprovalMode() { return approvalMode; }

  public void setApprovalMode(ApprovalMode approvalMode) { this.approvalMode = approvalMode; }

  public String getFinalApproverExternalUserId() { return finalApproverExternalUserId; }

  public void setFinalApproverExternalUserId(String value) {
    this.finalApproverExternalUserId = value;
  }

  public ResourceStatus getStatus() {
    return status;
  }

  public void setStatus(ResourceStatus status) {
    this.status = status;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
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
