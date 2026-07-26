package com.yanerdan.venueflow.resource.event;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yanerdan.venueflow.resource.event.persistence.ResourceOutboxEntity;
import com.yanerdan.venueflow.resource.event.persistence.ResourceOutboxMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence & resource-events")
public final class ResourceOutboxPublisher {

  private final ResourceOutboxMapper mapper;
  private final AtomicReference<RabbitTemplate> rabbit;
  private final String exchange;
  private final String routingKey;
  private final int batchSize;
  private final int maxAttempts;
  private final long confirmTimeoutMillis;

  public ResourceOutboxPublisher(
      ResourceOutboxMapper mapper,
      RabbitTemplate rabbit,
      @Value("${venueflow.resource-events.exchange}") String exchange,
      @Value("${venueflow.resource-events.routing-key}") String routingKey,
      @Value("${venueflow.resource-events.batch-size}") int batchSize,
      @Value("${venueflow.resource-events.max-attempts}") int maxAttempts,
      @Value("${venueflow.resource-events.confirm-timeout-ms}") long confirmTimeoutMillis) {
    this.mapper = mapper;
    this.rabbit = new AtomicReference<>(rabbit);
    this.exchange = exchange;
    this.routingKey = routingKey;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
    this.confirmTimeoutMillis = confirmTimeoutMillis;
  }

  @Scheduled(fixedDelayString = "${venueflow.resource-events.fixed-delay-ms}")
  public void publishBatch() {
    LocalDateTime now = LocalDateTime.now();
    List<ResourceOutboxEntity> events =
        mapper.selectList(
            Wrappers.<ResourceOutboxEntity>lambdaQuery()
                .in(ResourceOutboxEntity::getStatus, List.of("NEW", "RETRY"))
                .le(ResourceOutboxEntity::getNextAttemptAt, now)
                .orderByAsc(ResourceOutboxEntity::getId)
                .last("LIMIT " + Math.max(1, Math.min(batchSize, 200))));
    events.forEach(this::publish);
  }

  private void publish(ResourceOutboxEntity event) {
    try {
      CorrelationData correlation = new CorrelationData(event.getEventId());
      rabbit
          .get()
          .send(
              exchange,
              routingKey,
              MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                  .setMessageId(event.getEventId())
                  .setContentType("application/json")
                  .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                  .build(),
              correlation);
      CorrelationData.Confirm confirm =
          correlation.getFuture().get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);
      if (!confirm.ack() || correlation.getReturned() != null) {
        retry(event, "BROKER_REJECTED");
        return;
      }
      event.setStatus("PUBLISHED");
      event.setPublishedAt(LocalDateTime.now());
      event.setLastError(null);
      mapper.updateById(event);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      retry(event, "INTERRUPTED");
    } catch (ExecutionException | TimeoutException | AmqpException exception) {
      retry(event, "BROKER_UNAVAILABLE");
    }
  }

  private void retry(ResourceOutboxEntity event, String error) {
    int attempts = event.getAttempts() + 1;
    event.setAttempts(attempts);
    event.setStatus(attempts >= maxAttempts ? "DEAD" : "RETRY");
    event.setNextAttemptAt(
        LocalDateTime.now().plusSeconds(Math.min(300, 1L << Math.min(attempts, 8))));
    event.setLastError(error);
    mapper.updateById(event);
  }
}
