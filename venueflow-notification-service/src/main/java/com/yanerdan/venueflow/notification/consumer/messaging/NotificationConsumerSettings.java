package com.yanerdan.venueflow.notification.consumer.messaging;

import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("messaging")
public final class NotificationConsumerSettings {
  private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9._-]{1,128}");
  private final String consumerName;
  private final String sourceExchange;
  private final String workQueue;
  private final String retryExchange;
  private final String retryQueue;
  private final String deadExchange;
  private final String deadQueue;
  private final int prefetch;
  private final int concurrency;
  private final int retryDelayMillis;
  private final int maxAttempts;
  private final int maxMessageBytes;
  private final long confirmTimeoutMillis;

  public NotificationConsumerSettings(
      @Value("${venueflow.notification.consumer-name}") String consumerName,
      @Value("${venueflow.notification.source-exchange}") String sourceExchange,
      @Value("${venueflow.notification.work-queue}") String workQueue,
      @Value("${venueflow.notification.retry-exchange}") String retryExchange,
      @Value("${venueflow.notification.retry-queue}") String retryQueue,
      @Value("${venueflow.notification.dead-exchange}") String deadExchange,
      @Value("${venueflow.notification.dead-queue}") String deadQueue,
      @Value("${venueflow.notification.prefetch}") int prefetch,
      @Value("${venueflow.notification.concurrency}") int concurrency,
      @Value("${venueflow.notification.retry-delay-ms}") int retryDelayMillis,
      @Value("${venueflow.notification.max-attempts}") int maxAttempts,
      @Value("${venueflow.notification.max-message-bytes}") int maxMessageBytes,
      @Value("${venueflow.notification.confirm-timeout-ms}") long confirmTimeoutMillis) {
    this.consumerName = safeName(consumerName, "consumerName");
    this.sourceExchange = safeName(sourceExchange, "sourceExchange");
    this.workQueue = safeName(workQueue, "workQueue");
    this.retryExchange = safeName(retryExchange, "retryExchange");
    this.retryQueue = safeName(retryQueue, "retryQueue");
    this.deadExchange = safeName(deadExchange, "deadExchange");
    this.deadQueue = safeName(deadQueue, "deadQueue");
    this.prefetch = bounded(prefetch, 1, 100, "prefetch");
    this.concurrency = bounded(concurrency, 1, 16, "concurrency");
    this.retryDelayMillis = bounded(retryDelayMillis, 100, 600_000, "retryDelayMillis");
    this.maxAttempts = bounded(maxAttempts, 1, 10, "maxAttempts");
    this.maxMessageBytes = bounded(maxMessageBytes, 512, 65_536, "maxMessageBytes");
    this.confirmTimeoutMillis = bounded(confirmTimeoutMillis, 100, 30_000, "confirmTimeoutMillis");
  }

  private static String safeName(String value, String field) {
    if (value == null || !SAFE_NAME.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " is invalid");
    }
    return value;
  }

  private static int bounded(int value, int minimum, int maximum, String field) {
    if (value < minimum || value > maximum) {
      throw new IllegalArgumentException(field + " is outside the allowed range");
    }
    return value;
  }

  private static long bounded(long value, long minimum, long maximum, String field) {
    if (value < minimum || value > maximum) {
      throw new IllegalArgumentException(field + " is outside the allowed range");
    }
    return value;
  }

  public String consumerName() {
    return consumerName;
  }

  public String sourceExchange() {
    return sourceExchange;
  }

  public String workQueue() {
    return workQueue;
  }

  public String retryExchange() {
    return retryExchange;
  }

  public String retryQueue() {
    return retryQueue;
  }

  public String deadExchange() {
    return deadExchange;
  }

  public String deadQueue() {
    return deadQueue;
  }

  public int prefetch() {
    return prefetch;
  }

  public int concurrency() {
    return concurrency;
  }

  public int retryDelayMillis() {
    return retryDelayMillis;
  }

  public int maxAttempts() {
    return maxAttempts;
  }

  public int maxMessageBytes() {
    return maxMessageBytes;
  }

  public long confirmTimeoutMillis() {
    return confirmTimeoutMillis;
  }
}
