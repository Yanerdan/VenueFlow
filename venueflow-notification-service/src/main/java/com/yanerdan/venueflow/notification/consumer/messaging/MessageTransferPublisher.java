package com.yanerdan.venueflow.notification.consumer.messaging;

import com.yanerdan.venueflow.notification.consumer.domain.FailureCode;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
public class MessageTransferPublisher {
  public static final String ATTEMPT_HEADER = "x-venueflow-attempt";
  public static final String ERROR_HEADER = "x-venueflow-error-code";
  private final RabbitTemplate rabbitTemplate;
  private final NotificationConsumerSettings settings;

  public MessageTransferPublisher(
      RabbitTemplate rabbitTemplate, NotificationConsumerSettings settings) {
    this.rabbitTemplate = rabbitTemplate;
    this.settings = settings;
  }

  public TransferOutcome transfer(
      Message source, String exchange, String routingKey, int attempts, FailureCode failureCode) {
    var builder =
        MessageBuilder.withBody(source.getBody())
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .setHeader(ATTEMPT_HEADER, attempts)
            .setHeader(ERROR_HEADER, failureCode.name());
    var properties = source.getMessageProperties();
    if (properties.getContentType() != null) {
      builder.setContentType(properties.getContentType());
    }
    if (properties.getContentEncoding() != null) {
      builder.setContentEncoding(properties.getContentEncoding());
    }
    if (properties.getMessageId() != null) {
      builder.setMessageId(properties.getMessageId());
    }
    Message copy = builder.build();
    CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
    try {
      rabbitTemplate.send(exchange, routingKey, copy, correlation);
      CorrelationData.Confirm confirm =
          correlation.getFuture().get(settings.confirmTimeoutMillis(), TimeUnit.MILLISECONDS);
      if (correlation.getReturned() != null) {
        return TransferOutcome.UNROUTABLE;
      }
      return confirm.ack() ? TransferOutcome.CONFIRMED : TransferOutcome.CONFIRM_NACK;
    } catch (TimeoutException exception) {
      return TransferOutcome.CONFIRM_TIMEOUT;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return TransferOutcome.INTERRUPTED;
    } catch (ExecutionException | AmqpException exception) {
      return TransferOutcome.BROKER_UNAVAILABLE;
    }
  }
}
