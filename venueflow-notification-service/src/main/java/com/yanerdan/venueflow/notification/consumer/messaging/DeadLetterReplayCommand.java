package com.yanerdan.venueflow.notification.consumer.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.yanerdan.venueflow.notification.consumer.application.FailureAuditService;
import com.yanerdan.venueflow.notification.consumer.domain.BookingEvent;
import com.yanerdan.venueflow.notification.consumer.domain.BookingEventDecoder;
import com.yanerdan.venueflow.notification.consumer.domain.EnvelopeException;
import com.yanerdan.venueflow.notification.consumer.domain.FailureCode;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("messaging")
public class DeadLetterReplayCommand implements ApplicationRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(DeadLetterReplayCommand.class);
  private final CachingConnectionFactory connectionFactory;
  private final BookingEventDecoder decoder;
  private final MessageTransferPublisher transferPublisher;
  private final FailureAuditService auditService;
  private final NotificationConsumerSettings settings;
  private final String action;
  private final String expectedIdentity;
  private final String expectedFingerprint;
  private final String reason;
  private final boolean confirmed;

  public DeadLetterReplayCommand(
      CachingConnectionFactory connectionFactory,
      BookingEventDecoder decoder,
      MessageTransferPublisher transferPublisher,
      FailureAuditService auditService,
      NotificationConsumerSettings settings,
      @Value("${venueflow.notification.admin.action:}") String action,
      @Value("${venueflow.notification.admin.expected-identity:}") String expectedIdentity,
      @Value("${venueflow.notification.admin.expected-fingerprint:}") String expectedFingerprint,
      @Value("${venueflow.notification.admin.reason:}") String reason,
      @Value("${venueflow.notification.admin.confirm:false}") boolean confirmed) {
    this.connectionFactory = connectionFactory;
    this.decoder = decoder;
    this.transferPublisher = transferPublisher;
    this.auditService = auditService;
    this.settings = settings;
    this.action = action;
    this.expectedIdentity = expectedIdentity;
    this.expectedFingerprint = expectedFingerprint;
    this.reason = reason;
    this.confirmed = confirmed;
  }

  @Override
  public void run(ApplicationArguments arguments) throws IOException {
    if (action == null || action.isBlank()) {
      return;
    }
    if (!"PREVIEW_DLQ".equals(action) && !"REPLAY_DLQ".equals(action)) {
      throw new IllegalArgumentException("Unsupported notification admin action");
    }
    inspectHead("REPLAY_DLQ".equals(action));
  }

  private void inspectHead(boolean replay) throws IOException {
    Connection connection = connectionFactory.createConnection();
    Channel channel = connection.createChannel(false);
    try {
      GetResponse response = channel.basicGet(settings.deadQueue(), false);
      if (response == null) {
        LOGGER.info("notification_dlq empty=true");
        return;
      }
      long deliveryTag = response.getEnvelope().getDeliveryTag();
      String routingKey = response.getEnvelope().getRoutingKey();
      String fingerprint = BookingEventDecoder.rawFingerprint(response.getBody());
      Message message = toMessage(response);
      String eventId = safeEventId(message, routingKey);
      int attempts =
          headerInt(response.getProps().getHeaders(), MessageTransferPublisher.ATTEMPT_HEADER);
      String errorCode =
          headerText(response.getProps().getHeaders(), MessageTransferPublisher.ERROR_HEADER);
      LOGGER.info(
          "notification_dlq preview eventId={} fingerprint={} routingKey={} bytes={} attempt={} error={}",
          eventId,
          fingerprint,
          routingKey,
          response.getBody().length,
          attempts,
          errorCode);

      if (!replay) {
        channel.basicNack(deliveryTag, false, true);
        return;
      }
      requireReplayApproval(eventId, fingerprint);
      TransferOutcome outcome =
          transferPublisher.transfer(
              message, settings.sourceExchange(), routingKey, 0, FailureCode.PROCESSING_FAILED);
      if (outcome != TransferOutcome.CONFIRMED) {
        channel.basicNack(deliveryTag, false, true);
        throw new IllegalStateException("DLQ replay routing was not confirmed");
      }
      channel.basicAck(deliveryTag, false);
      auditService.recordReplay(settings.consumerName(), fingerprint, reason);
      LOGGER.info(
          "notification_dlq replayed=true eventId={} fingerprint={} reasonLength={}",
          eventId,
          fingerprint,
          reason.length());
    } finally {
      close(channel, connection);
    }
  }

  void requireReplayApproval(String eventId, String fingerprint) {
    if (!confirmed
        || expectedIdentity == null
        || !expectedIdentity.equals(eventId)
        || expectedFingerprint == null
        || !expectedFingerprint.equals(fingerprint)
        || reason == null
        || reason.isBlank()
        || reason.length() > 256) {
      throw new IllegalArgumentException(
          "DLQ replay requires confirmation, matching identity/fingerprint, and bounded reason");
    }
  }

  private String safeEventId(Message message, String routingKey) {
    try {
      BookingEvent event =
          decoder.decode(
              message.getBody(),
              routingKey,
              message.getMessageProperties().getContentType(),
              message.getMessageProperties().getContentEncoding());
      return event.eventId();
    } catch (EnvelopeException exception) {
      return "unknown";
    }
  }

  private static Message toMessage(GetResponse response) {
    var builder =
        MessageBuilder.withBody(response.getBody()).setDeliveryMode(MessageDeliveryMode.PERSISTENT);
    if (response.getProps().getContentType() != null) {
      builder.setContentType(response.getProps().getContentType());
    }
    if (response.getProps().getContentEncoding() != null) {
      builder.setContentEncoding(response.getProps().getContentEncoding());
    }
    if (response.getProps().getMessageId() != null) {
      builder.setMessageId(response.getProps().getMessageId());
    }
    Map<String, Object> headers = response.getProps().getHeaders();
    if (headers != null) {
      headers.forEach(builder::setHeader);
    }
    return builder.build();
  }

  private static int headerInt(Map<String, Object> headers, String name) {
    Object value = headers == null ? null : headers.get(name);
    return value instanceof Number number ? number.intValue() : 0;
  }

  private static String headerText(Map<String, Object> headers, String name) {
    Object value = headers == null ? null : headers.get(name);
    return value == null ? "unknown" : value.toString();
  }

  private static void close(Channel channel, Connection connection) throws IOException {
    try {
      if (channel != null && channel.isOpen()) {
        channel.close();
      }
    } catch (java.util.concurrent.TimeoutException exception) {
      throw new IOException("Timed out closing replay channel", exception);
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }
}
