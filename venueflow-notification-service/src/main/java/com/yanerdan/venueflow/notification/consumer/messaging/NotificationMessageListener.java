package com.yanerdan.venueflow.notification.consumer.messaging;

import com.rabbitmq.client.Channel;
import com.yanerdan.venueflow.notification.consumer.application.FailureAuditService;
import com.yanerdan.venueflow.notification.consumer.application.NotificationConsumerService;
import com.yanerdan.venueflow.notification.consumer.domain.BookingEvent;
import com.yanerdan.venueflow.notification.consumer.domain.BookingEventDecoder;
import com.yanerdan.venueflow.notification.consumer.domain.ConsumptionResult;
import com.yanerdan.venueflow.notification.consumer.domain.EnvelopeException;
import com.yanerdan.venueflow.notification.consumer.domain.FailureCode;
import com.yanerdan.venueflow.notification.consumer.domain.IdentityCollisionException;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("messaging")
public class NotificationMessageListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationMessageListener.class);
  private final BookingEventDecoder decoder;
  private final NotificationConsumerService consumerService;
  private final FailureAuditService failureAuditService;
  private final MessageTransferPublisher transferPublisher;
  private final NotificationConsumerSettings settings;
  private final MeterRegistry meterRegistry;
  private final AtomicLong lastMessageAgeSeconds = new AtomicLong();

  public NotificationMessageListener(
      BookingEventDecoder decoder,
      NotificationConsumerService consumerService,
      FailureAuditService failureAuditService,
      MessageTransferPublisher transferPublisher,
      NotificationConsumerSettings settings,
      MeterRegistry meterRegistry) {
    this.decoder = decoder;
    this.consumerService = consumerService;
    this.failureAuditService = failureAuditService;
    this.transferPublisher = transferPublisher;
    this.settings = settings;
    this.meterRegistry = meterRegistry;
    meterRegistry.gauge(
        "venueflow.notification.message.age.seconds", lastMessageAgeSeconds, AtomicLong::get);
  }

  @RabbitListener(
      queues = "${venueflow.notification.work-queue}",
      containerFactory = "notificationListenerContainerFactory")
  public void handle(Message message, Channel channel) throws IOException {
    meterRegistry.counter("venueflow.notification.received").increment();
    String routingKey = safeRoutingKey(message);
    BookingEvent event = null;
    try {
      event =
          decoder.decode(
              message.getBody(),
              routingKey,
              message.getMessageProperties().getContentType(),
              message.getMessageProperties().getContentEncoding());
      lastMessageAgeSeconds.set(
          Math.max(0L, Duration.between(event.occurredAt(), Instant.now()).toSeconds()));
      ConsumptionResult result = consumerService.consume(settings.consumerName(), event);
      channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
      meterRegistry.counter("venueflow.notification.ack").increment();
      meterRegistry
          .counter(
              result == ConsumptionResult.CONSUMED
                  ? "venueflow.notification.consumed"
                  : "venueflow.notification.duplicate")
          .increment();
    } catch (EnvelopeException exception) {
      transferFailure(message, channel, routingKey, null, exception.failureCode());
    } catch (IdentityCollisionException exception) {
      transferFailure(message, channel, routingKey, event, FailureCode.IDENTITY_COLLISION);
    } catch (RuntimeException exception) {
      transferFailure(message, channel, routingKey, event, FailureCode.PROCESSING_FAILED);
    }
  }

  private void transferFailure(
      Message message,
      Channel channel,
      String routingKey,
      BookingEvent event,
      FailureCode initialCode)
      throws IOException {
    int attempts = attempts(message);
    boolean terminal = initialCode.terminal() || attempts >= settings.maxAttempts();
    FailureCode finalCode =
        terminal && !initialCode.terminal() ? FailureCode.RETRY_EXHAUSTED : initialCode;
    int nextAttempt = terminal ? attempts : attempts + 1;
    String targetExchange = terminal ? settings.deadExchange() : settings.retryExchange();
    TransferOutcome outcome =
        transferPublisher.transfer(message, targetExchange, routingKey, nextAttempt, finalCode);
    String fingerprint = BookingEventDecoder.rawFingerprint(message.getBody());
    recordFailureBestEffort(event, fingerprint, routingKey, nextAttempt, finalCode);

    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    if (outcome == TransferOutcome.CONFIRMED) {
      channel.basicAck(deliveryTag, false);
      meterRegistry.counter("venueflow.notification.ack").increment();
      meterRegistry
          .counter(terminal ? "venueflow.notification.dead-letter" : "venueflow.notification.retry")
          .increment();
      LOGGER.warn(
          "notification_consumer outcome={} eventId={} routingKey={} attempt={}",
          finalCode,
          event == null ? "unknown" : event.eventId(),
          routingKey,
          nextAttempt);
      return;
    }

    channel.basicNack(deliveryTag, false, true);
    meterRegistry.counter("venueflow.notification.nack").increment();
    meterRegistry.counter("venueflow.notification.transfer-uncertain").increment();
    LOGGER.warn(
        "notification_consumer outcome=TRANSFER_UNCERTAIN routingKey={} attempt={}",
        routingKey,
        nextAttempt);
  }

  private void recordFailureBestEffort(
      BookingEvent event, String fingerprint, String routingKey, int attempts, FailureCode code) {
    try {
      failureAuditService.recordFailure(
          settings.consumerName(),
          event == null ? null : event.eventId(),
          fingerprint,
          routingKey,
          attempts,
          code);
    } catch (RuntimeException ignored) {
      meterRegistry.counter("venueflow.notification.failure-audit-unavailable").increment();
    }
  }

  private int attempts(Message message) {
    Object value =
        message.getMessageProperties().getHeaders().get(MessageTransferPublisher.ATTEMPT_HEADER);
    int attempts = value instanceof Number number ? number.intValue() : 0;
    return Math.max(0, Math.min(attempts, settings.maxAttempts()));
  }

  private static String safeRoutingKey(Message message) {
    String routingKey = message.getMessageProperties().getReceivedRoutingKey();
    return routingKey == null || routingKey.length() > 96 ? "invalid" : routingKey;
  }
}
