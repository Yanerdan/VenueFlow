package com.yanerdan.venueflow.notification.consumer.application;

import com.yanerdan.venueflow.notification.consumer.domain.BookingEvent;
import com.yanerdan.venueflow.notification.consumer.domain.ConsumedIdentity;
import com.yanerdan.venueflow.notification.consumer.domain.ConsumptionResult;
import com.yanerdan.venueflow.notification.consumer.domain.IdentityCollisionException;
import com.yanerdan.venueflow.notification.consumer.domain.NotificationDraft;
import com.yanerdan.venueflow.notification.consumer.persistence.NotificationConsumerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("persistence")
public class NotificationConsumerService {
  private final NotificationConsumerRepository repository;
  private final Clock clock;

  @Autowired
  public NotificationConsumerService(NotificationConsumerRepository repository) {
    this(repository, Clock.systemUTC());
  }

  NotificationConsumerService(NotificationConsumerRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional
  public ConsumptionResult consume(String consumerName, BookingEvent event) {
    Optional<ConsumedIdentity> existing = repository.findConsumed(consumerName, event.eventId());
    if (existing.isPresent()) {
      return duplicateOrCollision(event, existing.get());
    }

    Instant now = clock.instant();
    try {
      repository.insertConsumed(consumerName, event, now);
      repository.insertNotification(consumerName, event, NotificationDraft.from(event), now);
      return ConsumptionResult.CONSUMED;
    } catch (DuplicateKeyException exception) {
      ConsumedIdentity winner =
          repository
              .findConsumed(consumerName, event.eventId())
              .orElseThrow(IdentityCollisionException::new);
      return duplicateOrCollision(event, winner);
    }
  }

  private static ConsumptionResult duplicateOrCollision(
      BookingEvent event, ConsumedIdentity existing) {
    if (!event.sameIdentity(existing)) {
      throw new IdentityCollisionException();
    }
    return ConsumptionResult.DUPLICATE;
  }
}
