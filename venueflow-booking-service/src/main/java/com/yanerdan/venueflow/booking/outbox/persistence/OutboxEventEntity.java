package com.yanerdan.venueflow.booking.outbox.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yanerdan.venueflow.booking.outbox.domain.OutboxEvent;
import com.yanerdan.venueflow.booking.outbox.domain.OutboxStatus;
import java.time.LocalDateTime;

@TableName("booking_outbox_event")
public class OutboxEventEntity {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String eventId;
  private String aggregateType;
  private String aggregateId;
  private String eventType;
  private Integer eventVersion = 0;
  private String routingKey;
  private String payload;
  private String headers;
  private OutboxStatus status;
  private Integer retryCount = 0;
  private LocalDateTime nextRetryAt;
  private String claimToken;
  private LocalDateTime leaseUntil;
  private LocalDateTime createdAt;
  private LocalDateTime publishedAt;
  private String lastErrorCode;
  private Long version = 0L;

  public static OutboxEventEntity from(OutboxEvent event) {
    OutboxEventEntity entity = new OutboxEventEntity();
    entity.id = event.id();
    entity.eventId = event.eventId();
    entity.aggregateType = event.aggregateType();
    entity.aggregateId = event.aggregateId();
    entity.eventType = event.eventType();
    entity.eventVersion = event.eventVersion();
    entity.routingKey = event.routingKey();
    entity.payload = event.payload();
    entity.headers = event.headers();
    entity.status = event.status();
    entity.retryCount = event.retryCount();
    entity.nextRetryAt = event.nextRetryAt();
    entity.claimToken = event.claimToken();
    entity.leaseUntil = event.leaseUntil();
    entity.createdAt = event.createdAt();
    entity.publishedAt = event.publishedAt();
    entity.lastErrorCode = event.lastErrorCode();
    entity.version = event.version();
    return entity;
  }

  public OutboxEvent toDomain() {
    return new OutboxEvent(
        id,
        eventId,
        aggregateType,
        aggregateId,
        eventType,
        eventVersion,
        routingKey,
        payload,
        headers,
        status,
        retryCount,
        nextRetryAt,
        claimToken,
        leaseUntil,
        createdAt,
        publishedAt,
        lastErrorCode,
        version);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String value) {
    eventId = value;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public void setAggregateType(String value) {
    aggregateType = value;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public void setAggregateId(String value) {
    aggregateId = value;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String value) {
    eventType = value;
  }

  public Integer getEventVersion() {
    return eventVersion;
  }

  public void setEventVersion(Integer value) {
    eventVersion = value;
  }

  public String getRoutingKey() {
    return routingKey;
  }

  public void setRoutingKey(String value) {
    routingKey = value;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String value) {
    payload = value;
  }

  public String getHeaders() {
    return headers;
  }

  public void setHeaders(String value) {
    headers = value;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public void setStatus(OutboxStatus value) {
    status = value;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(Integer value) {
    retryCount = value;
  }

  public LocalDateTime getNextRetryAt() {
    return nextRetryAt;
  }

  public void setNextRetryAt(LocalDateTime value) {
    nextRetryAt = value;
  }

  public String getClaimToken() {
    return claimToken;
  }

  public void setClaimToken(String value) {
    claimToken = value;
  }

  public LocalDateTime getLeaseUntil() {
    return leaseUntil;
  }

  public void setLeaseUntil(LocalDateTime value) {
    leaseUntil = value;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime value) {
    createdAt = value;
  }

  public LocalDateTime getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(LocalDateTime value) {
    publishedAt = value;
  }

  public String getLastErrorCode() {
    return lastErrorCode;
  }

  public void setLastErrorCode(String value) {
    lastErrorCode = value;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long value) {
    version = value;
  }
}
