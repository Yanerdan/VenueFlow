package com.yanerdan.venueflow.booking.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class OutboxPublisherSettingsTest {
  @Test
  void validatesLeaseAndCapsBackoffWithoutOverflow() {
    OutboxPublisherSettings settings =
        new OutboxPublisherSettings("events", 20, 2_000, 5_000, 5, 1_000, 60_000, true);

    assertThat(settings.retryDelayMillis(30)).isEqualTo(60_000);
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new OutboxPublisherSettings("events", 20, 2_000, 2_999, 5, 1_000, 60_000, true));
  }
}
