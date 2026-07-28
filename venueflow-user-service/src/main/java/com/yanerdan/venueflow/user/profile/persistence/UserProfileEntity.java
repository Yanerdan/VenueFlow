package com.yanerdan.venueflow.user.profile.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_profile")
public class UserProfileEntity {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("external_user_id")
  private String externalUserId;

  @TableField("display_name")
  private String displayName;

  @TableField("campus_id")
  private String campusId;

  @TableField("identity_type")
  private String identityType;

  @TableField("department")
  private String department;

  @TableField("phone")
  private String phone;

  @TableField("email")
  private String email;

  @TableField("account_status")
  private String accountStatus;

  @TableField("booking_eligibility")
  private String bookingEligibility;

  @TableField("version")
  private Long version;

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

  public String getExternalUserId() {
    return externalUserId;
  }

  public void setExternalUserId(String externalUserId) {
    this.externalUserId = externalUserId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getCampusId() { return campusId; }

  public void setCampusId(String campusId) { this.campusId = campusId; }

  public String getIdentityType() { return identityType; }

  public void setIdentityType(String identityType) { this.identityType = identityType; }

  public String getDepartment() { return department; }

  public void setDepartment(String department) { this.department = department; }

  public String getPhone() { return phone; }

  public void setPhone(String phone) { this.phone = phone; }

  public String getEmail() { return email; }

  public void setEmail(String email) { this.email = email; }

  public String getAccountStatus() {
    return accountStatus;
  }

  public void setAccountStatus(String accountStatus) {
    this.accountStatus = accountStatus;
  }

  public String getBookingEligibility() {
    return bookingEligibility;
  }

  public void setBookingEligibility(String bookingEligibility) {
    this.bookingEligibility = bookingEligibility;
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
