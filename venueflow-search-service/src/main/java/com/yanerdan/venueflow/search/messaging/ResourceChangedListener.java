package com.yanerdan.venueflow.search.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.yanerdan.venueflow.search.application.SearchApplicationService;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("search")
public final class ResourceChangedListener {

  private final AtomicReference<ObjectMapper> objectMapper;
  private final SearchApplicationService service;

  public ResourceChangedListener(ObjectMapper objectMapper, SearchApplicationService service) {
    this.objectMapper = new AtomicReference<>(objectMapper);
    this.service = service;
  }

  @RabbitListener(
      queues = "${venueflow.search.queue}",
      containerFactory = "searchListenerContainerFactory")
  public void consume(Message message, Channel channel) throws IOException {
    long tag = message.getMessageProperties().getDeliveryTag();
    try {
      JsonNode event = objectMapper.get().readTree(message.getBody());
      requireEventType(event);
      service.project(
          requireText(event, "eventId"),
          event.path("payload").path("resourceId").longValue(),
          event.path("payload").path("resourceVersion").longValue());
      channel.basicAck(tag, false);
    } catch (RuntimeException exception) {
      channel.basicReject(tag, false);
    }
  }

  private static void requireEventType(JsonNode event) {
    if (!"resource.changed.v1".equals(requireText(event, "eventType"))) {
      throw new IllegalArgumentException("Unsupported resource event");
    }
  }

  private static String requireText(JsonNode event, String field) {
    String value = event.path(field).textValue();
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value;
  }
}
