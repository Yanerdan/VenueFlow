package com.yanerdan.venueflow.booking.outbox.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence")
@ConditionalOnProperty(name = "venueflow.outbox.admin.action")
public final class OutboxAdminCommand implements ApplicationRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxAdminCommand.class);
  private final OutboxAdminService service;
  private final String action;
  private final String eventId;
  private final String reason;
  private final boolean confirmed;

  public OutboxAdminCommand(
      OutboxAdminService service,
      @Value("${venueflow.outbox.admin.action}") String action,
      @Value("${venueflow.outbox.admin.event-id}") String eventId,
      @Value("${venueflow.outbox.admin.reason:}") String reason,
      @Value("${venueflow.outbox.admin.confirm:false}") boolean confirmed) {
    this.service = service;
    this.action = action;
    this.eventId = eventId;
    this.reason = reason;
    this.confirmed = confirmed;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if ("inspect".equals(action)) {
      LOGGER.info("Outbox metadata {}", service.inspect(eventId));
      return;
    }
    if ("requeue".equals(action)) {
      boolean accepted = service.requeue(eventId, reason, !confirmed, confirmed);
      LOGGER.info(
          "Outbox requeue eventId={} mode={} accepted={}",
          eventId,
          confirmed ? "confirmed" : "preview",
          accepted);
      return;
    }
    throw new IllegalArgumentException("Unsupported Outbox admin action");
  }
}
