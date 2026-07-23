package com.yanerdan.venueflow.notification.consumer.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("messaging")
public class QueueMetricsSampler {
  private final RabbitAdmin rabbitAdmin;
  private final NotificationConsumerSettings settings;
  private final AtomicLong workDepth = new AtomicLong();
  private final AtomicLong retryDepth = new AtomicLong();
  private final AtomicLong deadDepth = new AtomicLong();

  public QueueMetricsSampler(
      RabbitAdmin rabbitAdmin, NotificationConsumerSettings settings, MeterRegistry registry) {
    this.rabbitAdmin = rabbitAdmin;
    this.settings = settings;
    registry.gauge("venueflow.notification.queue.work.depth", workDepth, AtomicLong::get);
    registry.gauge("venueflow.notification.queue.retry.depth", retryDepth, AtomicLong::get);
    registry.gauge("venueflow.notification.queue.dead.depth", deadDepth, AtomicLong::get);
  }

  @Scheduled(fixedDelayString = "${venueflow.notification.queue-sample-delay-ms:10000}")
  public void sample() {
    workDepth.set(depth(settings.workQueue()));
    retryDepth.set(depth(settings.retryQueue()));
    deadDepth.set(depth(settings.deadQueue()));
  }

  private long depth(String queue) {
    Properties properties = rabbitAdmin.getQueueProperties(queue);
    if (properties == null) {
      return 0L;
    }
    Object value = properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
    return value instanceof Number number ? number.longValue() : 0L;
  }
}
