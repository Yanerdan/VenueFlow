package com.yanerdan.venueflow.notification.inbox;

import java.time.Instant;

public record NotificationInboxItem(
    long id,
    long userId,
    String bookingNo,
    String type,
    String title,
    String body,
    Instant createdAt) {}
