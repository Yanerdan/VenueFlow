package com.yanerdan.venueflow.booking.reconciliation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ReconciliationPropertiesTest {

  @Test
  void acceptsSafeDefaults() {
    ReconciliationProperties properties =
        properties(20, 8, Duration.ofSeconds(5), Duration.ofMinutes(15));

    assertThat(properties.enabled()).isFalse();

    assertThat(properties.batchSize()).isEqualTo(20);

    assertThat(properties.maxAttempts()).isEqualTo(8);
  }

  @Test
  void rejectsUnboundedBatchSize() {
    assertThatThrownBy(() -> properties(101, 8, Duration.ofSeconds(5), Duration.ofMinutes(15)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("batchSize");
  }

  @Test
  void rejectsBackoffThatShrinks() {
    assertThatThrownBy(() -> properties(20, 8, Duration.ofSeconds(10), Duration.ofSeconds(5)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxBackoff");
  }

  private static ReconciliationProperties properties(
      int batchSize, int maxAttempts, Duration initialBackoff, Duration maxBackoff) {
    return new ReconciliationProperties(
        false,
        batchSize,
        Duration.ofSeconds(30),
        Duration.ofSeconds(10),
        maxAttempts,
        initialBackoff,
        maxBackoff,
        Duration.ofSeconds(3),
        Duration.ofSeconds(2),
        Duration.ofSeconds(5));
  }
}
