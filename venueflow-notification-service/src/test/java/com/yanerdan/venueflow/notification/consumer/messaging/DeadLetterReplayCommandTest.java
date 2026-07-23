package com.yanerdan.venueflow.notification.consumer.messaging;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanerdan.venueflow.notification.consumer.application.FailureAuditService;
import com.yanerdan.venueflow.notification.consumer.domain.BookingEventDecoder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

class DeadLetterReplayCommandTest {
  private static final String EVENT_ID = "c88d6348-92c5-4ead-b14a-22557b60610d";
  private static final String FINGERPRINT = "abc123";

  @Test
  void requiresBothExpectedIdentityAndFingerprint() {
    DeadLetterReplayCommand command = command(EVENT_ID, FINGERPRINT, "operator replay", true);

    assertThatNoException().isThrownBy(() -> command.requireReplayApproval(EVENT_ID, FINGERPRINT));
    assertThatThrownBy(() -> command.requireReplayApproval(EVENT_ID, "different"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsReplayWithoutExplicitConfirmation() {
    DeadLetterReplayCommand command = command(EVENT_ID, FINGERPRINT, "operator replay", false);

    assertThatThrownBy(() -> command.requireReplayApproval(EVENT_ID, FINGERPRINT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static DeadLetterReplayCommand command(
      String expectedIdentity, String expectedFingerprint, String reason, boolean confirmed) {
    NotificationConsumerSettings settings =
        new NotificationConsumerSettings(
            "consumer",
            "source",
            "work",
            "retry",
            "retry.queue",
            "dead",
            "dead.queue",
            10,
            1,
            1000,
            3,
            4096,
            2000);
    return new DeadLetterReplayCommand(
        Mockito.mock(CachingConnectionFactory.class),
        new BookingEventDecoder(new ObjectMapper(), 4096),
        Mockito.mock(MessageTransferPublisher.class),
        Mockito.mock(FailureAuditService.class),
        settings,
        "REPLAY_DLQ",
        expectedIdentity,
        expectedFingerprint,
        reason,
        confirmed);
  }
}
