package com.yanerdan.venueflow.resource.event.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("resource_outbox")
public class ResourceOutboxEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String eventId;
  private Long resourceId;
  private Long aggregateVersion;
  private String eventType;
  private String payload;
  private String status;
  private Integer attempts;
  private LocalDateTime nextAttemptAt;
  private LocalDateTime publishedAt;
  private String lastError;

  public Long getId() {
    return id;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String value) {
    eventId = value;
  }

  public Long getResourceId() {
    return resourceId;
  }

  public void setResourceId(Long value) {
    resourceId = value;
  }

  public Long getAggregateVersion() {
    return aggregateVersion;
  }

  public void setAggregateVersion(Long value) {
    aggregateVersion = value;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String value) {
    eventType = value;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String value) {
    payload = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  public Integer getAttempts() {
    return attempts;
  }

  public void setAttempts(Integer value) {
    attempts = value;
  }

  public LocalDateTime getNextAttemptAt() {
    return nextAttemptAt;
  }

  public void setNextAttemptAt(LocalDateTime value) {
    nextAttemptAt = value;
  }

  public LocalDateTime getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(LocalDateTime value) {
    publishedAt = value;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String value) {
    lastError = value;
  }
}
