package com.yanerdan.venueflow.booking.expiration.config;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "venueflow.booking.expiration")
public record ExpirationProperties(
    boolean enabled,
    Duration confirmationWindow,
    int batchSize,
    Duration leaseDuration,
    Duration scanDelay,
    int maxAttempts,
    Duration initialBackoff,
    Duration maxBackoff,
    Duration operationLookupTimeout,
    Duration connectTimeout,
    Duration requestTimeout) {

  public ExpirationProperties {
    range(batchSize, 1, 100, "batchSize");
    range(maxAttempts, 1, 20, "maxAttempts");
    duration(
        confirmationWindow, Duration.ofSeconds(30), Duration.ofHours(24), "confirmationWindow");
    duration(leaseDuration, Duration.ofSeconds(5), Duration.ofMinutes(5), "leaseDuration");
    duration(scanDelay, Duration.ofSeconds(1), Duration.ofMinutes(5), "scanDelay");
    duration(initialBackoff, Duration.ofSeconds(1), Duration.ofMinutes(5), "initialBackoff");
    duration(maxBackoff, Duration.ofSeconds(5), Duration.ofHours(24), "maxBackoff");
    duration(
        operationLookupTimeout,
        Duration.ofMillis(100),
        Duration.ofSeconds(30),
        "operationLookupTimeout");
    duration(connectTimeout, Duration.ofMillis(100), Duration.ofSeconds(10), "connectTimeout");
    duration(requestTimeout, Duration.ofMillis(500), Duration.ofSeconds(60), "requestTimeout");
    if (maxBackoff.compareTo(initialBackoff) < 0 || requestTimeout.compareTo(connectTimeout) < 0) {
      throw new IllegalArgumentException("expiration duration bounds conflict");
    }
  }

  private static void range(int value, int min, int max, String name) {
    if (value < min || value > max) {
      throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
    }
  }

  private static void duration(Duration value, Duration min, Duration max, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
      throw new IllegalArgumentException(name + " is outside its allowed range");
    }
  }
}
