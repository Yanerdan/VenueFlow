package com.yanerdan.venueflow.resource.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanerdan.venueflow.resource.catalog.application.ResourceResult;
import com.yanerdan.venueflow.resource.event.persistence.ResourceOutboxEntity;
import com.yanerdan.venueflow.resource.event.persistence.ResourceOutboxMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence & resource-events")
public final class PersistentResourceChangeRecorder implements ResourceChangeRecorder {

  private final ResourceOutboxMapper mapper;
  private final AtomicReference<ObjectMapper> objectMapper;

  public PersistentResourceChangeRecorder(ResourceOutboxMapper mapper, ObjectMapper objectMapper) {
    this.mapper = mapper;
    this.objectMapper = new AtomicReference<>(objectMapper);
  }

  @Override
  public void record(ResourceResult resource) {
    String eventId = UUID.randomUUID().toString();
    Instant now = Instant.now();
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("eventId", eventId);
    envelope.put("eventType", "resource.changed.v1");
    envelope.put("eventVersion", 1);
    envelope.put("aggregateId", resource.id().toString());
    envelope.put("aggregateVersion", resource.version());
    envelope.put("occurredAt", now.toString());
    envelope.put("producer", "venueflow-resource-service");
    envelope.put("traceId", canonicalTrace(MDC.get("traceId")));
    envelope.put(
        "payload", Map.of("resourceId", resource.id(), "resourceVersion", resource.version()));

    ResourceOutboxEntity entity = new ResourceOutboxEntity();
    entity.setEventId(eventId);
    entity.setResourceId(resource.id());
    entity.setAggregateVersion(resource.version());
    entity.setEventType("resource.changed.v1");
    entity.setPayload(toJson(envelope));
    entity.setStatus("NEW");
    entity.setAttempts(0);
    entity.setNextAttemptAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC));
    if (mapper.insert(entity) != 1) {
      throw new IllegalStateException("Resource change Outbox insert failed");
    }
  }

  private String toJson(Map<String, Object> value) {
    try {
      return objectMapper.get().writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Resource change event serialization failed", exception);
    }
  }

  private static String canonicalTrace(String candidate) {
    try {
      return UUID.fromString(candidate).toString();
    } catch (RuntimeException exception) {
      return UUID.randomUUID().toString();
    }
  }
}
