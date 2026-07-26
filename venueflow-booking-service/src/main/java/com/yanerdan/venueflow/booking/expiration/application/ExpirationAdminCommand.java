package com.yanerdan.venueflow.booking.expiration.application;

import com.yanerdan.venueflow.booking.expiration.domain.TimeoutReservation;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence & expiration")
public class ExpirationAdminCommand implements ApplicationRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExpirationAdminCommand.class);
  private final ExpirationService service;
  private final String action;
  private final String reason;
  private final boolean confirmed;

  public ExpirationAdminCommand(
      ExpirationService service,
      @Value("${venueflow.booking.expiration.admin.action:}") String action,
      @Value("${venueflow.booking.expiration.admin.reason:}") String reason,
      @Value("${venueflow.booking.expiration.admin.confirm:false}") boolean confirmed) {
    this.service = service;
    this.action = action;
    this.reason = reason;
    this.confirmed = confirmed;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if (action == null || action.isBlank()) return;
    if ("PREVIEW".equals(action)) {
      List<TimeoutReservation> due = service.preview();
      due.forEach(
          timeout ->
              LOGGER.info(
                  "booking_expiration_preview bookingNo={} attempt={}",
                  timeout.bookingNo(),
                  timeout.attemptCount()));
      return;
    }
    if (!"RUN".equals(action)) {
      throw new IllegalArgumentException("Unsupported expiration admin action");
    }
    if (!confirmed || reason == null || reason.isBlank() || reason.length() > 256) {
      throw new IllegalArgumentException(
          "Expiration RUN requires confirmation and a bounded reason");
    }
    service.runOnce("OPERATOR", reason);
  }
}
