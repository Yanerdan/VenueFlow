package com.yanerdan.venueflow.booking.expiration.application;

import com.yanerdan.venueflow.booking.expiration.config.ExpirationProperties;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence & expiration")
public class ExpirationScheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExpirationScheduler.class);
  private final ExpirationService service;
  private final ExpirationProperties properties;
  private final AtomicBoolean running = new AtomicBoolean(true);

  public ExpirationScheduler(ExpirationService service, ExpirationProperties properties) {
    this.service = service;
    this.properties = properties;
  }

  @Scheduled(
      fixedDelayString = "${venueflow.booking.expiration.scan-delay:PT10S}",
      initialDelayString = "${venueflow.booking.expiration.scan-delay:PT10S}")
  void scan() {
    if (running.get() && properties.enabled()) {
      service.runOnce("SCHEDULED", null);
    }
  }

  @PreDestroy
  void stop() {
    running.set(false);
    LOGGER.info("booking_expiration outcome=SHUTDOWN newClaims=false");
  }
}
