package com.yanerdan.venueflow.notification.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.yanerdan.venueflow.notification.consumer.domain.BookingEventDecoder;
import com.yanerdan.venueflow.notification.consumer.domain.BookingEventDecoderTest;
import com.yanerdan.venueflow.notification.consumer.domain.FailureCode;
import com.yanerdan.venueflow.notification.consumer.messaging.MessageTransferPublisher;
import com.yanerdan.venueflow.notification.consumer.messaging.NotificationConsumerSettings;
import com.yanerdan.venueflow.notification.consumer.messaging.TransferOutcome;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "spring.profiles.active=persistence,messaging",
      "venueflow.notification.retry-delay-ms=1500",
      "venueflow.notification.max-attempts=2",
      "venueflow.notification.queue-sample-delay-ms=60000"
    })
class NotificationConsumerSuite {
  @Container
  private static final MySQLContainer MYSQL =
      new MySQLContainer("mysql:8.4.10")
          .withDatabaseName("venueflow_notification")
          .withUsername("notification")
          .withPassword("notification-test");

  @Container
  private static final RabbitMQContainer RABBIT =
      new RabbitMQContainer("rabbitmq:4.1.8-management");

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private RabbitTemplate rabbitTemplate;
  @Autowired private RabbitAdmin rabbitAdmin;
  @Autowired private NotificationConsumerSettings settings;
  @Autowired private MessageTransferPublisher transferPublisher;

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry registry) {
    registry.add("VENUEFLOW_NOTIFICATION_DB_URL", MYSQL::getJdbcUrl);
    registry.add("VENUEFLOW_NOTIFICATION_DB_USERNAME", MYSQL::getUsername);
    registry.add("VENUEFLOW_NOTIFICATION_DB_PASSWORD", MYSQL::getPassword);
    registry.add("VENUEFLOW_RABBITMQ_HOST", RABBIT::getHost);
    registry.add("VENUEFLOW_RABBITMQ_PORT", RABBIT::getAmqpPort);
    registry.add("VENUEFLOW_RABBITMQ_USERNAME", RABBIT::getAdminUsername);
    registry.add("VENUEFLOW_RABBITMQ_PASSWORD", RABBIT::getAdminPassword);
    registry.add("VENUEFLOW_RABBITMQ_VHOST", () -> "/");
  }

  @BeforeEach
  void reset() {
    rabbitAdmin.purgeQueue(settings.workQueue(), true);
    rabbitAdmin.purgeQueue(settings.retryQueue(), true);
    rabbitAdmin.purgeQueue(settings.deadQueue(), true);
    jdbcTemplate.update("DELETE FROM notification_consume_failure");
    jdbcTemplate.update("DELETE FROM notification_record");
    jdbcTemplate.update("DELETE FROM notification_consumed_event");
  }

  @Test
  void migrationTopologyManualAckAndDuplicateConverge() {
    assertThat(tableCount("notification_consumed_event")).isEqualTo(1);
    assertThat(queueDepth(settings.workQueue())).isZero();

    String eventId = UUID.randomUUID().toString();
    Message message = confirmation(eventId);
    rabbitTemplate.send(settings.sourceExchange(), BookingEventDecoder.CONFIRMED_ROUTE, message);
    rabbitTemplate.send(settings.sourceExchange(), BookingEventDecoder.CONFIRMED_ROUTE, message);

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              assertThat(count("notification_consumed_event")).isEqualTo(1);
              assertThat(count("notification_record")).isEqualTo(1);
              assertThat(queueDepth(settings.workQueue())).isZero();
            });
  }

  @Test
  void transientFailureUsesRetryThenCommitsAfterRecovery() {
    jdbcTemplate.execute("RENAME TABLE notification_record TO notification_record_offline");
    try {
      rabbitTemplate.send(
          settings.sourceExchange(),
          BookingEventDecoder.CONFIRMED_ROUTE,
          confirmation(UUID.randomUUID().toString()));
      await()
          .atMost(Duration.ofSeconds(10))
          .untilAsserted(() -> assertThat(queueDepth(settings.retryQueue())).isEqualTo(1));
    } finally {
      jdbcTemplate.execute("RENAME TABLE notification_record_offline TO notification_record");
    }

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              assertThat(count("notification_consumed_event")).isEqualTo(1);
              assertThat(count("notification_record")).isEqualTo(1);
            });
  }

  @Test
  void poisonMessageMovesToDeadLetterQueue() {
    Message poison =
        MessageBuilder.withBody("not-json".getBytes(StandardCharsets.UTF_8))
            .setContentType("application/json")
            .setContentEncoding("UTF-8")
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .build();
    rabbitTemplate.send(settings.sourceExchange(), BookingEventDecoder.CONFIRMED_ROUTE, poison);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              assertThat(queueDepth(settings.deadQueue())).isEqualTo(1);
              assertThat(count("notification_record")).isZero();
            });
  }

  @Test
  void confirmedReplayReturnsDeadLetterMessageToWorkPath() {
    Message event = confirmation(UUID.randomUUID().toString());
    rabbitTemplate.send(settings.deadExchange(), BookingEventDecoder.CONFIRMED_ROUTE, event);
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(queueDepth(settings.deadQueue())).isEqualTo(1));

    Message dead = rabbitTemplate.receive(settings.deadQueue(), 5_000);
    assertThat(dead).isNotNull();
    assertThat(
            transferPublisher.transfer(
                dead,
                settings.sourceExchange(),
                BookingEventDecoder.CONFIRMED_ROUTE,
                0,
                FailureCode.PROCESSING_FAILED))
        .isEqualTo(TransferOutcome.CONFIRMED);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(count("notification_record")).isEqualTo(1));
  }

  @Test
  void expirationDeliveryIsIdempotent() {
    String eventId = UUID.randomUUID().toString();
    Message event = expiration(eventId);
    rabbitTemplate.send(settings.sourceExchange(), BookingEventDecoder.EXPIRED_ROUTE, event);
    rabbitTemplate.send(settings.sourceExchange(), BookingEventDecoder.EXPIRED_ROUTE, event);

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              assertThat(count("notification_consumed_event")).isEqualTo(1);
              assertThat(count("notification_record")).isEqualTo(1);
              assertThat(
                      jdbcTemplate.queryForObject(
                          "SELECT notification_type FROM notification_record", String.class))
                  .isEqualTo("BOOKING_EXPIRED");
            });
  }

  @Test
  void completionDeliveryIsIdempotent() {
    String eventId = UUID.randomUUID().toString();
    Message event = completion(eventId);
    rabbitTemplate.send(settings.sourceExchange(), BookingEventDecoder.COMPLETED_ROUTE, event);
    rabbitTemplate.send(settings.sourceExchange(), BookingEventDecoder.COMPLETED_ROUTE, event);

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              assertThat(count("notification_consumed_event")).isEqualTo(1);
              assertThat(count("notification_record")).isEqualTo(1);
              assertThat(
                      jdbcTemplate.queryForObject(
                          "SELECT notification_type FROM notification_record", String.class))
                  .isEqualTo("BOOKING_COMPLETED");
            });
  }

  private Message confirmation(String eventId) {
    return MessageBuilder.withBody(
            BookingEventDecoderTest.confirmation(eventId).getBytes(StandardCharsets.UTF_8))
        .setMessageId(eventId)
        .setContentType("application/json")
        .setContentEncoding("UTF-8")
        .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
        .build();
  }

  private Message expiration(String eventId) {
    return MessageBuilder.withBody(
            BookingEventDecoderTest.expiration(eventId).getBytes(StandardCharsets.UTF_8))
        .setMessageId(eventId)
        .setContentType("application/json")
        .setContentEncoding("UTF-8")
        .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
        .build();
  }

  private Message completion(String eventId) {
    return MessageBuilder.withBody(
            BookingEventDecoderTest.completion(eventId).getBytes(StandardCharsets.UTF_8))
        .setMessageId(eventId)
        .setContentType("application/json")
        .setContentEncoding("UTF-8")
        .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
        .build();
  }

  private int count(String table) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
  }

  private int tableCount(String table) {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = ?
        """,
        Integer.class,
        table);
  }

  private int queueDepth(String queue) {
    Properties properties = rabbitAdmin.getQueueProperties(queue);
    assertThat(properties).isNotNull();
    return ((Number) properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).intValue();
  }
}
