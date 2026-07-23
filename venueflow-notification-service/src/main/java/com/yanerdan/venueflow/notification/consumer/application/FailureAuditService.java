package com.yanerdan.venueflow.notification.consumer.application;

import com.yanerdan.venueflow.notification.consumer.domain.FailureCode;
import com.yanerdan.venueflow.notification.consumer.persistence.NotificationConsumerRepository;
import java.time.Clock;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("persistence")
public class FailureAuditService {
  private final NotificationConsumerRepository repository;
  private final Clock clock = Clock.systemUTC();

  public FailureAuditService(NotificationConsumerRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailure(
      String consumerName,
      String eventId,
      String fingerprint,
      String routingKey,
      int attempts,
      FailureCode code) {
    repository.insertFailure(
        consumerName, eventId, fingerprint, routingKey, attempts, code, clock.instant());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordReplay(String consumerName, String fingerprint, String reason) {
    repository.recordReplay(consumerName, fingerprint, reason, clock.instant());
  }
}
