package com.yanerdan.venueflow.booking.reconciliation.application;

import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence & reconciliation")
public class ReconciliationAdminCommand implements ApplicationRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliationAdminCommand.class);
  private final ReconciliationService service;
  private final String action;
  private final String reason;
  private final boolean confirmed;

  public ReconciliationAdminCommand(
      ReconciliationService service,
      @Value("${venueflow.booking.reconciliation.admin.action:}") String action,
      @Value("${venueflow.booking.reconciliation.admin.reason:}") String reason,
      @Value("${venueflow.booking.reconciliation.admin.confirm:false}") boolean confirmed) {
    this.service = service;
    this.action = action;
    this.reason = reason;
    this.confirmed = confirmed;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if (action == null || action.isBlank()) {
      return;
    }
    if ("PREVIEW".equals(action)) {
      List<ReconciliationIntent> due = service.preview();
      due.forEach(
          intent ->
              LOGGER.info(
                  "booking_reconciliation_preview intentId={} workflow={} requestId={} attempt={}",
                  intent.id(),
                  intent.workflowType(),
                  intent.requestId(),
                  intent.attemptCount()));
      return;
    }
    if (!"RUN".equals(action)) {
      throw new IllegalArgumentException("Unsupported reconciliation admin action");
    }
    if (!confirmed || reason == null || reason.isBlank() || reason.length() > 256) {
      throw new IllegalArgumentException(
          "Reconciliation RUN requires confirmation and a bounded reason");
    }
    service.runOnce("OPERATOR", reason);
  }
}
