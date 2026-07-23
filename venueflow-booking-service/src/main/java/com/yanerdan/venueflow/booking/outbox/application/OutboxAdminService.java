package com.yanerdan.venueflow.booking.outbox.application;

import com.yanerdan.venueflow.booking.outbox.domain.OutboxEvent;
import com.yanerdan.venueflow.booking.outbox.persistence.OutboxRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("persistence")
public class OutboxAdminService {
  private final OutboxRepository repository;

  public OutboxAdminService(OutboxRepository repository) {
    this.repository = repository;
  }

  public OutboxEventMetadata inspect(String eventId) {
    OutboxEvent event = repository.find(requireEventId(eventId));
    return event == null ? null : OutboxEventMetadata.from(event);
  }

  public boolean requeue(
      String eventId, String operatorReason, boolean preview, boolean confirmed) {
    requireReason(operatorReason);
    OutboxEvent event = repository.find(requireEventId(eventId));
    if (preview) {
      return event != null
          && event.status() == com.yanerdan.venueflow.booking.outbox.domain.OutboxStatus.DEAD;
    }
    return repository.requeue(eventId, operatorReason, confirmed);
  }

  private static String requireEventId(String eventId) {
    if (eventId == null || eventId.isBlank()) {
      throw new IllegalArgumentException("Event ID is required");
    }
    return eventId;
  }

  private static void requireReason(String operatorReason) {
    if (operatorReason == null || operatorReason.isBlank()) {
      throw new IllegalArgumentException("Operator reason is required");
    }
  }
}
