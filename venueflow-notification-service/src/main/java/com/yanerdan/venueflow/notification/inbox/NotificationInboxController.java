package com.yanerdan.venueflow.notification.inbox;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Profile("persistence")
@RequestMapping("/api/v1/notifications")
public class NotificationInboxController {

  private final NotificationInboxService service;

  public NotificationInboxController(NotificationInboxService service) {
    this.service = service;
  }

  @GetMapping
  public SuccessEnvelope<NotificationInboxPage> findByUser(
      @RequestParam @Positive long userId,
      @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
      @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
    return new SuccessEnvelope<>(
        "OK", "success", service.findByUser(userId, pageNumber, pageSize), canonicalTrace(traceId));
  }

  private static String canonicalTrace(String value) {
    try {
      return UUID.fromString(value).toString();
    } catch (RuntimeException exception) {
      return UUID.randomUUID().toString();
    }
  }

  public record SuccessEnvelope<T>(String code, String message, T data, String traceId) {}
}
