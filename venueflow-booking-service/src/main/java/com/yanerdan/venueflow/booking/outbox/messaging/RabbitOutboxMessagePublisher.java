package com.yanerdan.venueflow.booking.outbox.messaging;

import com.yanerdan.venueflow.booking.outbox.application.OutboxMessagePublisher;
import com.yanerdan.venueflow.booking.outbox.application.OutboxPublishOutcome;
import com.yanerdan.venueflow.booking.outbox.application.OutboxPublisherSettings;
import com.yanerdan.venueflow.booking.outbox.domain.OutboxEvent;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("messaging")
public class RabbitOutboxMessagePublisher implements OutboxMessagePublisher {
  private final AtomicReference<RabbitTemplate> rabbitTemplate;
  private final OutboxPublisherSettings settings;

  public RabbitOutboxMessagePublisher(
      RabbitTemplate rabbitTemplate, OutboxPublisherSettings settings) {
    this.rabbitTemplate = new AtomicReference<>(rabbitTemplate);
    this.settings = settings;
  }

  @Override
  public OutboxPublishOutcome publish(OutboxEvent event) {
    CorrelationData correlation = new CorrelationData(event.eventId());
    Message message =
        MessageBuilder.withBody(event.payload().getBytes(StandardCharsets.UTF_8))
            .setMessageId(event.eventId())
            .setContentType("application/json")
            .setContentEncoding(StandardCharsets.UTF_8.name())
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .build();
    try {
      rabbitTemplate.get().send(settings.exchange(), event.routingKey(), message, correlation);
      CorrelationData.Confirm confirm =
          correlation.getFuture().get(settings.confirmTimeoutMillis(), TimeUnit.MILLISECONDS);
      if (correlation.getReturned() != null) {
        return OutboxPublishOutcome.UNROUTABLE;
      }
      return confirm.ack() ? OutboxPublishOutcome.CONFIRMED : OutboxPublishOutcome.CONFIRM_NACK;
    } catch (TimeoutException exception) {
      return OutboxPublishOutcome.CONFIRM_TIMEOUT;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return OutboxPublishOutcome.PUBLISH_INTERRUPTED;
    } catch (ExecutionException | AmqpException exception) {
      return OutboxPublishOutcome.BROKER_UNAVAILABLE;
    }
  }
}
