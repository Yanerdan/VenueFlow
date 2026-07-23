package com.yanerdan.venueflow.notification.consumer.persistence;

import com.yanerdan.venueflow.notification.consumer.domain.BookingEvent;
import com.yanerdan.venueflow.notification.consumer.domain.ConsumedIdentity;
import com.yanerdan.venueflow.notification.consumer.domain.FailureCode;
import com.yanerdan.venueflow.notification.consumer.domain.NotificationDraft;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("persistence")
public class NotificationConsumerRepository {
  private final JdbcTemplate jdbcTemplate;

  public NotificationConsumerRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<ConsumedIdentity> findConsumed(String consumerName, String eventId) {
    return jdbcTemplate
        .query(
            """
            SELECT event_type, event_version, payload_hash
            FROM notification_consumed_event
            WHERE consumer_name = ? AND event_id = ?
            """,
            (resultSet, rowNumber) ->
                new ConsumedIdentity(
                    resultSet.getString("event_type"),
                    resultSet.getInt("event_version"),
                    resultSet.getString("payload_hash")),
            consumerName,
            eventId)
        .stream()
        .findFirst();
  }

  public void insertConsumed(String consumerName, BookingEvent event, Instant consumedAt) {
    jdbcTemplate.update(
        """
        INSERT INTO notification_consumed_event
          (consumer_name, event_id, event_type, event_version, payload_hash, result, consumed_at)
        VALUES (?, ?, ?, ?, ?, 'CONSUMED', ?)
        """,
        consumerName,
        event.eventId(),
        event.eventType(),
        event.eventVersion(),
        event.payloadHash(),
        Timestamp.from(consumedAt));
  }

  public void insertNotification(
      String consumerName, BookingEvent event, NotificationDraft draft, Instant createdAt) {
    jdbcTemplate.update(
        """
        INSERT INTO notification_record
          (consumer_name, event_id, user_id, booking_no, notification_type, title, body, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        consumerName,
        event.eventId(),
        event.userId(),
        event.bookingNo(),
        draft.type(),
        draft.title(),
        draft.body(),
        Timestamp.from(createdAt));
  }

  public void insertFailure(
      String consumerName,
      String eventId,
      String fingerprint,
      String routingKey,
      int attempts,
      FailureCode code,
      Instant failedAt) {
    jdbcTemplate.update(
        """
        INSERT INTO notification_consume_failure
          (consumer_name, event_id, message_fingerprint, routing_key, attempt_count, error_code,
           terminal, first_failed_at, last_failed_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        consumerName,
        eventId,
        fingerprint,
        routingKey,
        attempts,
        code.name(),
        code.terminal(),
        Timestamp.from(failedAt),
        Timestamp.from(failedAt));
  }

  public void recordReplay(
      String consumerName, String fingerprint, String reason, Instant replayedAt) {
    jdbcTemplate.update(
        """
        UPDATE notification_consume_failure
        SET replay_reason = ?, replayed_at = ?
        WHERE consumer_name = ? AND message_fingerprint = ? AND terminal = TRUE
        ORDER BY id DESC
        LIMIT 1
        """,
        reason,
        Timestamp.from(replayedAt),
        consumerName,
        fingerprint);
  }
}
