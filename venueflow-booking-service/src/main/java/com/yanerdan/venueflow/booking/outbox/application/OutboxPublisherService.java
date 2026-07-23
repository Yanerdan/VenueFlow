package com.yanerdan.venueflow.booking.outbox.application;

import com.yanerdan.venueflow.booking.outbox.domain.OutboxEvent;
import com.yanerdan.venueflow.booking.outbox.persistence.OutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("messaging")
public class OutboxPublisherService {
  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisherService.class);
  private final OutboxRepository repository;
  private final OutboxMessagePublisher publisher;
  private final OutboxPublisherSettings settings;
  private final AtomicReference<MeterRegistry> meters;

  public OutboxPublisherService(
      OutboxRepository repository,
      OutboxMessagePublisher publisher,
      OutboxPublisherSettings settings,
      MeterRegistry meters) {
    this.repository = repository;
    this.publisher = publisher;
    this.settings = settings;
    this.meters = new AtomicReference<>(meters);
    Gauge.builder("venueflow.outbox.backlog", repository, OutboxRepository::backlogCount)
        .register(meters);
    Gauge.builder(
            "venueflow.outbox.oldest.eligible.age.seconds",
            repository,
            OutboxRepository::oldestEligibleAgeSeconds)
        .register(meters);
  }

  @Scheduled(fixedDelayString = "${venueflow.outbox.scan-delay-ms}")
  public void scanScheduled() {
    if (settings.enabled()) {
      scanOnce();
    }
  }

  public int scanOnce() {
    List<OutboxEvent> events = repository.claimBatch(settings.batchSize(), settings.leaseMillis());
    meters.get().counter("venueflow.outbox.claimed").increment(events.size());
    for (OutboxEvent event : events) {
      publish(event);
    }
    return events.size();
  }

  private void publish(OutboxEvent event) {
    OutboxPublishOutcome outcome = publisher.publish(event);
    boolean finalized;
    if (outcome == OutboxPublishOutcome.CONFIRMED) {
      finalized = repository.markPublished(event.eventId(), event.claimToken());
      if (finalized) {
        meters.get().counter("venueflow.outbox.confirmed").increment();
      }
    } else {
      finalized =
          repository.markFailed(
              event.eventId(),
              event.claimToken(),
              outcome.name(),
              event.retryCount(),
              settings.maxAttempts(),
              settings.retryDelayMillis(event.retryCount()));
      meters.get().counter("venueflow.outbox.failed", "outcome", outcome.name()).increment();
      if (outcome == OutboxPublishOutcome.UNROUTABLE) {
        meters.get().counter("venueflow.outbox.returned").increment();
      }
      if (finalized) {
        String result = event.retryCount() + 1 >= settings.maxAttempts() ? "dead" : "retry";
        meters.get().counter("venueflow.outbox." + result).increment();
      }
    }
    LOGGER.info(
        "Outbox publish outcome eventId={} claimToken={} outcome={} finalized={}",
        event.eventId(),
        event.claimToken(),
        outcome,
        finalized);
  }
}
