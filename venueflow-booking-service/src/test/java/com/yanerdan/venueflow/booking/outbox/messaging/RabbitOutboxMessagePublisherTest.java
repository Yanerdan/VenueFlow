package com.yanerdan.venueflow.booking.outbox.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.yanerdan.venueflow.booking.outbox.application.OutboxPublishOutcome;
import com.yanerdan.venueflow.booking.outbox.application.OutboxPublisherSettings;
import com.yanerdan.venueflow.booking.outbox.domain.OutboxEvent;
import com.yanerdan.venueflow.booking.outbox.domain.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitOutboxMessagePublisherTest {
  private final RabbitTemplate template = mock(RabbitTemplate.class);

  @Test
  void mapsAckAndNack() {
    completeConfirm(true);
    assertThat(publisher(100).publish(event())).isEqualTo(OutboxPublishOutcome.CONFIRMED);

    completeConfirm(false);
    assertThat(publisher(100).publish(event())).isEqualTo(OutboxPublishOutcome.CONFIRM_NACK);
  }

  @Test
  void mapsTimeoutAndPreservesInterrupt() {
    assertThat(publisher(1).publish(event())).isEqualTo(OutboxPublishOutcome.CONFIRM_TIMEOUT);

    Thread.currentThread().interrupt();
    try {
      assertThat(publisher(100).publish(event()))
          .isEqualTo(OutboxPublishOutcome.PUBLISH_INTERRUPTED);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  private void completeConfirm(boolean ack) {
    doAnswer(
            invocation -> {
              CorrelationData correlation = invocation.getArgument(3);
              correlation.getFuture().complete(new CorrelationData.Confirm(ack, null));
              return null;
            })
        .when(template)
        .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
  }

  private RabbitOutboxMessagePublisher publisher(long timeout) {
    return new RabbitOutboxMessagePublisher(
        template,
        new OutboxPublisherSettings("events", 1, timeout, timeout + 1_000, 3, 1, 10, true));
  }

  private static OutboxEvent event() {
    return new OutboxEvent(
        1L,
        "04ea8095-a80b-4d5c-a8a6-6a312e685bb8",
        "BOOKING",
        "B-1",
        "BOOKING_RESERVATION_CONFIRMED",
        1,
        "booking.reservation.confirmed.v1",
        "{}",
        "{}",
        OutboxStatus.PUBLISHING,
        0,
        null,
        "31f3192e-d627-498f-b018-82b88e633f40",
        null,
        null,
        null,
        null,
        1);
  }
}
