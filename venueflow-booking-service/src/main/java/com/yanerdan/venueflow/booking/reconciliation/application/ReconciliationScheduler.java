package com.yanerdan.venueflow.booking.reconciliation.application;

import com.yanerdan.venueflow.booking.reconciliation.config.ReconciliationProperties;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence & reconciliation")
public class ReconciliationScheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliationScheduler.class);
  private final ReconciliationService service;
  private final ReconciliationProperties properties;
  private final AtomicBoolean running = new AtomicBoolean(true);

  public ReconciliationScheduler(
      ReconciliationService service, ReconciliationProperties properties) {
    this.service = service;
    this.properties = properties;
  }

  @Scheduled(
      fixedDelayString = "${venueflow.booking.reconciliation.scan-delay:PT10S}",
      initialDelayString = "${venueflow.booking.reconciliation.scan-delay:PT10S}")
  void scan() {
    if (running.get() && properties.enabled()) {
      service.runOnce("SCHEDULED", null);
    }
  }

  @PreDestroy
  void stop() {
    running.set(false);
    LOGGER.info("booking_reconciliation outcome=SHUTDOWN newClaims=false");
  }
}
