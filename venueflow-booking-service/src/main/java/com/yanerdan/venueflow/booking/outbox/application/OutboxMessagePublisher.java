package com.yanerdan.venueflow.booking.outbox.application;

import com.yanerdan.venueflow.booking.outbox.domain.OutboxEvent;

public interface OutboxMessagePublisher {
  OutboxPublishOutcome publish(OutboxEvent event);
}
