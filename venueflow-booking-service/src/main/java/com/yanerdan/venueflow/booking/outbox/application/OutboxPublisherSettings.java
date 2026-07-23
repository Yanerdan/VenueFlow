package com.yanerdan.venueflow.booking.outbox.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("messaging")
public final class OutboxPublisherSettings {
  private final String exchange;
  private final int batchSize;
  private final long confirmTimeoutMillis;
  private final long leaseMillis;
  private final int maxAttempts;
  private final long initialBackoffMillis;
  private final long maxBackoffMillis;
  private final boolean enabled;

  public OutboxPublisherSettings(
      @Value("${venueflow.outbox.exchange}") String exchange,
      @Value("${venueflow.outbox.batch-size}") int batchSize,
      @Value("${venueflow.outbox.confirm-timeout-ms}") long confirmTimeoutMillis,
      @Value("${venueflow.outbox.lease-ms}") long leaseMillis,
      @Value("${venueflow.outbox.max-attempts}") int maxAttempts,
      @Value("${venueflow.outbox.initial-backoff-ms}") long initialBackoffMillis,
      @Value("${venueflow.outbox.max-backoff-ms}") long maxBackoffMillis,
      @Value("${venueflow.outbox.enabled}") boolean enabled) {
    if (exchange == null
        || exchange.isBlank()
        || batchSize < 1
        || batchSize > 100
        || confirmTimeoutMillis < 1
        || confirmTimeoutMillis > 30_000
        || leaseMillis < confirmTimeoutMillis + 1_000
        || leaseMillis > 300_000
        || maxAttempts < 1
        || maxAttempts > 100
        || initialBackoffMillis < 1
        || maxBackoffMillis < initialBackoffMillis
        || maxBackoffMillis > 86_400_000) {
      throw new IllegalArgumentException("Invalid bounded Outbox publisher configuration");
    }
    this.exchange = exchange;
    this.batchSize = batchSize;
    this.confirmTimeoutMillis = confirmTimeoutMillis;
    this.leaseMillis = leaseMillis;
    this.maxAttempts = maxAttempts;
    this.initialBackoffMillis = initialBackoffMillis;
    this.maxBackoffMillis = maxBackoffMillis;
    this.enabled = enabled;
  }

  public String exchange() {
    return exchange;
  }

  public int batchSize() {
    return batchSize;
  }

  public long confirmTimeoutMillis() {
    return confirmTimeoutMillis;
  }

  public long leaseMillis() {
    return leaseMillis;
  }

  public int maxAttempts() {
    return maxAttempts;
  }

  public boolean enabled() {
    return enabled;
  }

  public long retryDelayMillis(int retryCount) {
    long multiplier = 1L << Math.min(retryCount, 30);
    return initialBackoffMillis > maxBackoffMillis / multiplier
        ? maxBackoffMillis
        : initialBackoffMillis * multiplier;
  }
}
