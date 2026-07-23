package com.yanerdan.venueflow.notification.consumer.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.yanerdan.venueflow.notification.consumer.application.FailureAuditService;
import com.yanerdan.venueflow.notification.consumer.application.NotificationConsumerService;
import com.yanerdan.venueflow.notification.consumer.domain.BookingEventDecoder;
import com.yanerdan.venueflow.notification.consumer.domain.ConsumptionResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

class NotificationMessageListenerTest {
  private final NotificationConsumerService consumerService =
      Mockito.mock(NotificationConsumerService.class);
  private final FailureAuditService auditService = Mockito.mock(FailureAuditService.class);
  private final MessageTransferPublisher publisher = Mockito.mock(MessageTransferPublisher.class);
  private final NotificationConsumerSettings settings =
      new NotificationConsumerSettings(
          "consumer",
          "venueflow.events.v1",
          "venueflow.notification.booking.v1",
          "venueflow.notification.retry.v1",
          "venueflow.notification.booking.retry.v1",
          "venueflow.dead.v1",
          "venueflow.notification.booking.dlq.v1",
          10,
          1,
          1000,
          3,
          4096,
          2000);
  private final NotificationMessageListener listener =
      new NotificationMessageListener(
          new BookingEventDecoder(new ObjectMapper(), 4096),
          consumerService,
          auditService,
          publisher,
          settings,
          new SimpleMeterRegistry());
  private final Channel channel = Mockito.mock(Channel.class);

  @Test
  void commitsThenAcknowledgesValidMessage() throws Exception {
    Message message = validMessage();
    when(consumerService.consume(eq("consumer"), any())).thenReturn(ConsumptionResult.CONSUMED);

    listener.handle(message, channel);

    verify(consumerService).consume(eq("consumer"), any());
    verify(channel).basicAck(7L, false);
    verify(publisher, never()).transfer(any(), any(), any(), anyInt(), any());
  }

  @Test
  void poisonMessageMovesToDeadQueueBeforeAck() throws Exception {
    Message message =
        MessageBuilder.withBody("bad".getBytes(StandardCharsets.UTF_8))
            .setContentType("application/json")
            .setContentEncoding("UTF-8")
            .setReceivedRoutingKey(BookingEventDecoder.CONFIRMED_ROUTE)
            .setDeliveryTag(8L)
            .build();
    when(publisher.transfer(eq(message), eq(settings.deadExchange()), any(), eq(0), any()))
        .thenReturn(TransferOutcome.CONFIRMED);

    listener.handle(message, channel);

    verify(channel).basicAck(8L, false);
  }

  @Test
  void uncertainRetryTransferNacksOriginal() throws Exception {
    Message message = validMessage();
    when(consumerService.consume(eq("consumer"), any()))
        .thenThrow(new IllegalStateException("database unavailable"));
    when(publisher.transfer(eq(message), eq(settings.retryExchange()), any(), eq(1), any()))
        .thenReturn(TransferOutcome.CONFIRM_TIMEOUT);

    listener.handle(message, channel);

    verify(channel).basicNack(7L, false, true);
  }

  private static Message validMessage() {
    return MessageBuilder.withBody(
            com.yanerdan.venueflow.notification.consumer.domain.BookingEventDecoderTest
                .confirmation("c88d6348-92c5-4ead-b14a-22557b60610d")
                .getBytes(StandardCharsets.UTF_8))
        .setContentType("application/json")
        .setContentEncoding("UTF-8")
        .setReceivedRoutingKey(BookingEventDecoder.CONFIRMED_ROUTE)
        .setDeliveryTag(7L)
        .build();
  }
}
