package com.yanerdan.venueflow.booking.expiration.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ExpirationPropertiesTest {
  @Test
  void acceptsBoundedDefaults() {
    properties(20, 8);
  }

  @Test
  void rejectsUnboundedBatchAndAttempts() {
    assertThatThrownBy(() -> properties(0, 8)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> properties(20, 21)).isInstanceOf(IllegalArgumentException.class);
  }

  private static ExpirationProperties properties(int batch, int attempts) {
    return new ExpirationProperties(
        false,
        Duration.ofMinutes(15),
        batch,
        Duration.ofSeconds(30),
        Duration.ofSeconds(10),
        attempts,
        Duration.ofSeconds(5),
        Duration.ofMinutes(15),
        Duration.ofSeconds(3),
        Duration.ofSeconds(2),
        Duration.ofSeconds(5));
  }
}
