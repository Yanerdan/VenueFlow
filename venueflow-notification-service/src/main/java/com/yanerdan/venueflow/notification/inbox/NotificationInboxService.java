package com.yanerdan.venueflow.notification.inbox;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("persistence")
public class NotificationInboxService {

  private final NotificationInboxRepository repository;

  public NotificationInboxService(NotificationInboxRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public NotificationInboxPage findByUser(long userId, int pageNumber, int pageSize) {
    if (userId <= 0 || pageNumber < 0 || pageSize < 1 || pageSize > 100) {
      throw new IllegalArgumentException("Invalid notification inbox page");
    }
    return repository.findByUser(userId, pageNumber, pageSize);
  }
}
