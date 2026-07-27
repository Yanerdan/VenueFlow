package com.yanerdan.venueflow.notification.inbox;

import java.util.List;

public record NotificationInboxPage(
    List<NotificationInboxItem> items, long totalElements, int pageNumber, int pageSize) {
  public NotificationInboxPage {
    items = List.copyOf(items);
  }
}
