package com.yanerdan.venueflow.notification.inbox;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("persistence")
public class NotificationInboxRepository {

  private final JdbcTemplate jdbcTemplate;

  public NotificationInboxRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public NotificationInboxPage findByUser(long userId, int pageNumber, int pageSize) {
    long offset = Math.multiplyExact((long) pageNumber, pageSize);
    List<NotificationInboxItem> items =
        jdbcTemplate.query(
            """
            SELECT id, user_id, booking_no, notification_type, title, body, created_at
            FROM notification_record
            WHERE user_id = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """,
            (resultSet, rowNumber) ->
                new NotificationInboxItem(
                    resultSet.getLong("id"),
                    resultSet.getLong("user_id"),
                    resultSet.getString("booking_no"),
                    resultSet.getString("notification_type"),
                    resultSet.getString("title"),
                    resultSet.getString("body"),
                    resultSet.getTimestamp("created_at").toInstant()),
            userId,
            pageSize,
            offset);
    Long total =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notification_record WHERE user_id = ?", Long.class, userId);
    return new NotificationInboxPage(items, total == null ? 0 : total, pageNumber, pageSize);
  }
}
