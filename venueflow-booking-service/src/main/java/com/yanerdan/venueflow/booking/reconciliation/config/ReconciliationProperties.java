package com.yanerdan.venueflow.booking.reconciliation.config;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "venueflow.booking.reconciliation")
public record ReconciliationProperties(
    boolean enabled,
    int batchSize,
    Duration leaseDuration,
    Duration scanDelay,
    int maxAttempts,
    Duration initialBackoff,
    Duration maxBackoff,
    Duration operationLookupTimeout,
    Duration connectTimeout,
    Duration requestTimeout) {

  public ReconciliationProperties {
    requireRange(batchSize, 1, 100, "batchSize");

    requireRange(maxAttempts, 1, 20, "maxAttempts");

    requireDuration(leaseDuration, Duration.ofSeconds(5), Duration.ofMinutes(5), "leaseDuration");

    requireDuration(scanDelay, Duration.ofSeconds(1), Duration.ofMinutes(5), "scanDelay");

    requireDuration(initialBackoff, Duration.ofSeconds(1), Duration.ofMinutes(5), "initialBackoff");

    requireDuration(maxBackoff, Duration.ofSeconds(5), Duration.ofHours(24), "maxBackoff");

    requireDuration(
        operationLookupTimeout,
        Duration.ofMillis(100),
        Duration.ofSeconds(30),
        "operationLookupTimeout");

    requireDuration(
        connectTimeout, Duration.ofMillis(100), Duration.ofSeconds(10), "connectTimeout");

    requireDuration(
        requestTimeout, Duration.ofMillis(500), Duration.ofSeconds(60), "requestTimeout");

    if (maxBackoff.compareTo(initialBackoff) < 0) {
      throw new IllegalArgumentException("maxBackoff must not be shorter " + "than initialBackoff");
    }

    if (requestTimeout.compareTo(connectTimeout) < 0) {
      throw new IllegalArgumentException(
          "requestTimeout must not be shorter " + "than connectTimeout");
    }
  }

  private static void requireRange(int value, int minimum, int maximum, String name) {
    if (value < minimum || value > maximum) {
      throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
    }
  }

  private static void requireDuration(
      Duration value, Duration minimum, Duration maximum, String name) {
    Objects.requireNonNull(value, name + " must not be null");

    if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
      throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
    }
  }
}
