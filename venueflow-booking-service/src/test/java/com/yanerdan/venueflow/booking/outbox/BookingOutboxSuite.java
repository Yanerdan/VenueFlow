package com.yanerdan.venueflow.booking.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient;
import com.yanerdan.venueflow.booking.collaboration.UserEligibilityClient;
import com.yanerdan.venueflow.booking.outbox.application.OutboxMessagePublisher;
import com.yanerdan.venueflow.booking.outbox.application.OutboxPublishOutcome;
import com.yanerdan.venueflow.booking.outbox.application.OutboxPublisherService;
import com.yanerdan.venueflow.booking.outbox.domain.OutboxEvent;
import com.yanerdan.venueflow.booking.outbox.persistence.OutboxRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"persistence", "messaging"})
class BookingOutboxSuite {
  private static final String EXCHANGE = "venueflow.events.v1";
  private static final String QUEUE = "venueflow.outbox.test";

  @Container
  private static final MySQLContainer MYSQL =
      new MySQLContainer("mysql:8.4.10")
          .withDatabaseName("venueflow_booking")
          .withUsername("venueflow_booking_app")
          .withPassword("booking-test-password");

  @Container
  private static final RabbitMQContainer RABBIT =
      new RabbitMQContainer("rabbitmq:4.1.8-management")
          .withAdminUser("venueflow")
          .withAdminPassword("outbox-test-password");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private RabbitAdmin rabbitAdmin;
  @Autowired private RabbitTemplate rabbitTemplate;
  @Autowired private OutboxPublisherService scanner;
  @Autowired private OutboxRepository repository;
  @Autowired private OutboxMessagePublisher publisher;
  @MockitoBean private UserEligibilityClient userClient;
  @MockitoBean private ResourceCapacityClient resourceClient;

  private Binding binding;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.rabbitmq.host", RABBIT::getHost);
    registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
    registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    registry.add("venueflow.collaborators.user-base-url", () -> "http://127.0.0.1:1");
    registry.add("venueflow.collaborators.resource-base-url", () -> "http://127.0.0.1:1");
    registry.add("venueflow.outbox.enabled", () -> "false");
    registry.add("venueflow.outbox.confirm-timeout-ms", () -> "3000");
    registry.add("venueflow.outbox.lease-ms", () -> "6000");
    registry.add("venueflow.outbox.initial-backoff-ms", () -> "1");
  }

  @BeforeEach
  void clean() {
    jdbcTemplate.update("DELETE FROM booking_outbox_event");
    jdbcTemplate.update("DELETE FROM booking_idempotency");
    jdbcTemplate.update("DELETE FROM booking_reservation");
    when(userClient.isBookingPermitted(1L)).thenReturn(true);
    rabbitAdmin.deleteQueue(QUEUE);
    Queue queue = new Queue(QUEUE, false, false, false);
    TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
    rabbitAdmin.declareExchange(exchange);
    rabbitAdmin.declareQueue(queue);
    binding = BindingBuilder.bind(queue).to(exchange).with("booking.reservation.#");
    rabbitAdmin.declareBinding(binding);
  }

  @Test
  void publishesPersistentRoutedMessageAndMarksItPublished() throws Exception {
    createBooking("97f0d23e-5098-44c8-9ccb-7918456e1921");

    assertThat(scanner.scanOnce()).isEqualTo(1);
    Message message = rabbitTemplate.receive(QUEUE, Duration.ofSeconds(3).toMillis());

    assertThat(message).isNotNull();
    assertThat(message.getMessageProperties().getMessageId()).hasSize(36);
    assertThat(message.getMessageProperties().getReceivedDeliveryMode())
        .isEqualTo(MessageDeliveryMode.PERSISTENT);
    assertThat(jdbcTemplate.queryForObject("SELECT status FROM booking_outbox_event", String.class))
        .isEqualTo("PUBLISHED");
  }

  @Test
  void mandatoryReturnSchedulesRetry() throws Exception {
    rabbitAdmin.removeBinding(binding);
    createBooking("f40dc435-1618-4468-b0f4-cf6d5aee3356");

    assertThat(scanner.scanOnce()).isEqualTo(1);

    assertThat(
            jdbcTemplate.queryForMap(
                "SELECT status, retry_count, last_error_code FROM booking_outbox_event"))
        .containsEntry("status", "RETRY")
        .containsEntry("retry_count", 1)
        .containsEntry("last_error_code", "UNROUTABLE");
  }

  @Test
  void aLeaseHasOneOwnerAndStaleTokenCannotFinalize() throws Exception {
    createBooking("3f660518-752c-41b9-814f-c73c6dc67079");

    List<OutboxEvent> first = repository.claimBatch(1, 6_000);
    List<OutboxEvent> second = repository.claimBatch(1, 6_000);

    assertThat(first).hasSize(1);
    assertThat(second).isEmpty();
    assertThat(repository.markPublished(first.getFirst().eventId(), "stale-token")).isFalse();
  }

  @Test
  void expiredPostConfirmLeaseRepublishesSameEventId() throws Exception {
    createBooking("658eb59c-a8f0-4170-9696-f31278b41154");
    OutboxEvent firstClaim = repository.claimBatch(1, 6_000).getFirst();

    assertThat(publisher.publish(firstClaim)).isEqualTo(OutboxPublishOutcome.CONFIRMED);
    jdbcTemplate.update(
        "UPDATE booking_outbox_event SET lease_until = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND");
    assertThat(scanner.scanOnce()).isEqualTo(1);

    Message first = rabbitTemplate.receive(QUEUE, Duration.ofSeconds(3).toMillis());
    Message second = rabbitTemplate.receive(QUEUE, Duration.ofSeconds(3).toMillis());
    assertThat(first).isNotNull();
    assertThat(second).isNotNull();
    assertThat(first.getMessageProperties().getMessageId())
        .isEqualTo(second.getMessageProperties().getMessageId())
        .isEqualTo(firstClaim.eventId());
  }

  private void createBooking(String key) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/bookings")
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content("{\"userId\":1,\"slotId\":2,\"quantity\":1}"))
        .andExpect(status().isCreated());
  }
}
