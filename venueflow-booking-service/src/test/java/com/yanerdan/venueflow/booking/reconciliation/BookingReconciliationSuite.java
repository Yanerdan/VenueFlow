package com.yanerdan.venueflow.booking.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yanerdan.venueflow.booking.reconciliation.application.ReconciliationService;
import com.yanerdan.venueflow.booking.reconciliation.application.port.ReconciliationIntentRepository;
import com.yanerdan.venueflow.booking.reconciliation.application.port.ReconciliationIntentRepository.ClaimedIntents;
import com.yanerdan.venueflow.booking.reconciliation.domain.NewReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationIntent;
import com.yanerdan.venueflow.booking.reconciliation.domain.ReconciliationWorkflowType;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"persistence", "reconciliation"})
@Import(BookingReconciliationSuite.JsonConfiguration.class)
class BookingReconciliationSuite {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final AtomicBoolean ALLOCATION_VISIBLE = new AtomicBoolean();
  private static final AtomicBoolean RELEASE_VISIBLE = new AtomicBoolean();
  private static final AtomicBoolean SLOW_RELEASE = new AtomicBoolean();
  private static final AtomicBoolean RESOURCE_UNAVAILABLE = new AtomicBoolean();
  private static final AtomicBoolean CONFLICTING_OPERATION = new AtomicBoolean();
  private static final AtomicInteger RELEASE_CALLS = new AtomicInteger();
  private static final HttpServer RESOURCE_SERVER = startResourceServer();

  @Container
  private static final MySQLContainer MYSQL =
      new MySQLContainer("mysql:8.4.10")
          .withDatabaseName("venueflow_booking")
          .withUsername("venueflow_booking_app")
          .withPassword("booking-test-password");

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ReconciliationIntentRepository intents;
  @Autowired private ReconciliationService reconciliation;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("venueflow.collaborators.user-base-url", () -> "http://127.0.0.1:1");
    registry.add(
        "venueflow.collaborators.resource-base-url",
        () -> "http://127.0.0.1:" + RESOURCE_SERVER.getAddress().getPort());
    registry.add("venueflow.collaborators.connect-timeout-ms", () -> 200);
    registry.add("venueflow.collaborators.request-timeout-ms", () -> 500);
    registry.add("venueflow.collaborators.lookup-attempts", () -> 1);
    registry.add("venueflow.booking.reconciliation.enabled", () -> false);
    registry.add("venueflow.booking.reconciliation.batch-size", () -> 10);
    registry.add("venueflow.booking.reconciliation.lease-duration", () -> "PT30S");
    registry.add("venueflow.booking.reconciliation.scan-delay", () -> "PT10S");
    registry.add("venueflow.booking.reconciliation.max-attempts", () -> 3);
    registry.add("venueflow.booking.reconciliation.initial-backoff", () -> "PT1S");
    registry.add("venueflow.booking.reconciliation.max-backoff", () -> "PT1M");
    registry.add("venueflow.booking.reconciliation.operation-lookup-timeout", () -> "PT3S");
    registry.add("venueflow.booking.reconciliation.connect-timeout", () -> "PT1S");
    registry.add("venueflow.booking.reconciliation.request-timeout", () -> "PT5S");
  }

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM repair_action");
    jdbc.update("DELETE FROM reconciliation_issue");
    jdbc.update("DELETE FROM reconciliation_run");
    jdbc.update("DELETE FROM booking_reconciliation_intent");
    jdbc.update("DELETE FROM booking_outbox_event");
    jdbc.update("DELETE FROM booking_status_log");
    jdbc.update("DELETE FROM booking_idempotency");
    jdbc.update("DELETE FROM booking_reservation");
    ALLOCATION_VISIBLE.set(false);
    RELEASE_VISIBLE.set(false);
    SLOW_RELEASE.set(false);
    RESOURCE_UNAVAILABLE.set(false);
    CONFLICTING_OPERATION.set(false);
    RELEASE_CALLS.set(0);
  }

  @AfterAll
  static void stopServer() {
    RESOURCE_SERVER.stop(0);
  }

  @TestConfiguration
  static class JsonConfiguration {
    @Bean
    ObjectMapper objectMapper() {
      return JSON;
    }
  }

  @Test
  void migratesAndReclaimsExpiredLease() {
    assertThat(
            jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE script = 'V003__add_booking_reconciliation.sql' AND success = 1
                """,
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN ('booking_reconciliation_intent',
                    'reconciliation_run', 'reconciliation_issue', 'repair_action')
                """,
                Integer.class))
        .isEqualTo(4);

    createAllocationIntent("lease-test");
    List<ReconciliationIntent> first =
        intents
            .claimDue(LocalDateTime.now(), 1, "worker-one", LocalDateTime.now().plusSeconds(30))
            .intents();
    assertThat(first).hasSize(1);
    assertThat(
            intents
                .claimDue(LocalDateTime.now(), 1, "worker-two", LocalDateTime.now().plusSeconds(30))
                .intents())
        .isEmpty();

    jdbc.update(
        "UPDATE booking_reconciliation_intent SET lease_expires_at = ? WHERE id = ?",
        LocalDateTime.now().minusSeconds(1),
        first.getFirst().id());
    ClaimedIntents reclaimed =
        intents.claimDue(LocalDateTime.now(), 1, "worker-two", LocalDateTime.now().plusSeconds(30));
    assertThat(reclaimed.leaseReclaimed()).isEqualTo(1);
    assertThat(reclaimed.intents())
        .singleElement()
        .satisfies(intent -> assertThat(intent.leaseOwner()).isEqualTo("worker-two"));
  }

  @Test
  void provesAndReleasesOrphanAfterLostResponse() {
    ALLOCATION_VISIBLE.set(true);
    SLOW_RELEASE.set(true);
    createAllocationIntent("orphan-test");

    reconciliation.runOnce("OPERATOR", "integration-test");

    assertThat(intentValue("state")).isEqualTo("RESOLVED");
    assertThat(intentValue("outcome_code")).isEqualTo("ORPHAN_RELEASED");
    assertThat(RELEASE_CALLS).hasValue(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM repair_action WHERE status = 'SUCCEEDED'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void completesCancellationOnceAfterProvenRelease() {
    String requestId = UUID.randomUUID().toString();
    String allocationId = "allocate:" + requestId;
    String releaseId = "release:" + requestId;
    LocalDateTime now = LocalDateTime.now();
    jdbc.update(
        """
        INSERT INTO booking_reservation
          (booking_no, request_id, user_id, slot_id, quantity, status,
           allocation_operation_id, release_operation_id, version,
           created_at, confirmed_at, updated_at)
        VALUES (?, ?, 1, 2, 1, 'CONFIRMED', ?, ?, 0, ?, ?, ?)
        """,
        "B-" + requestId,
        requestId,
        allocationId,
        releaseId,
        now,
        now,
        now);
    Long bookingId =
        jdbc.queryForObject(
            "SELECT id FROM booking_reservation WHERE request_id = ?", Long.class, requestId);
    intents.create(
        new NewReconciliationIntent(
            ReconciliationWorkflowType.RELEASE,
            requestId,
            bookingId,
            2,
            1,
            allocationId,
            releaseId,
            now.minusSeconds(1)));
    RELEASE_VISIBLE.set(true);

    reconciliation.runOnce("OPERATOR", "integration-test");
    reconciliation.runOnce("OPERATOR", "integration-test-replay");

    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM booking_reservation WHERE id = ?", String.class, bookingId))
        .isEqualTo("CANCELLED");
    assertThat(intentValue("state")).isEqualTo("RESOLVED");
    assertThat(
            jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM booking_outbox_event
                WHERE event_type = 'BOOKING_RESERVATION_CANCELLED'
                """,
                Integer.class))
        .isEqualTo(1);
    assertThat(RELEASE_CALLS).hasValue(0);
  }

  @Test
  void deduplicatesOutageIssueAndExhaustsBoundedAttempts() {
    RESOURCE_UNAVAILABLE.set(true);
    createAllocationIntent("outage-test");

    reconciliation.runOnce("OPERATOR", "outage-one");
    assertThat(intentValue("state")).isEqualTo("OPEN");
    assertThat(
            jdbc.queryForObject(
                "SELECT next_check_at > UTC_TIMESTAMP(6) FROM booking_reconciliation_intent",
                Boolean.class))
        .isTrue();
    forceDue();
    reconciliation.runOnce("OPERATOR", "outage-two");
    forceDue();
    reconciliation.runOnce("OPERATOR", "outage-three");

    assertThat(intentValue("state")).isEqualTo("EXHAUSTED");
    assertThat(intentValue("outcome_code")).isEqualTo("ATTEMPTS_EXHAUSTED");
    assertThat(
            jdbc.queryForObject("SELECT occurrence_count FROM reconciliation_issue", Integer.class))
        .isEqualTo(3);
  }

  @Test
  void recordsConflictWithoutRepairWrite() {
    ALLOCATION_VISIBLE.set(true);
    CONFLICTING_OPERATION.set(true);
    createAllocationIntent("conflict-test");

    reconciliation.runOnce("OPERATOR", "conflict");

    assertThat(intentValue("state")).isEqualTo("OPEN");
    assertThat(intentValue("last_error_code")).isEqualTo("OPERATION_MISMATCH");
    assertThat(RELEASE_CALLS).hasValue(0);
  }

  private void createAllocationIntent(String suffix) {
    intents.create(
        new NewReconciliationIntent(
            ReconciliationWorkflowType.ALLOCATE,
            suffix,
            null,
            2,
            1,
            "allocate:" + suffix,
            "release:" + suffix,
            LocalDateTime.now().minusSeconds(1)));
  }

  private String intentValue(String column) {
    return jdbc.queryForObject(
        "SELECT " + column + " FROM booking_reconciliation_intent", String.class);
  }

  private void forceDue() {
    jdbc.update(
        """
        UPDATE booking_reconciliation_intent
        SET next_check_at = ?, updated_at = ?
        WHERE state = 'OPEN'
        """,
        LocalDateTime.now().minusSeconds(1),
        LocalDateTime.now());
  }

  private static HttpServer startResourceServer() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/api/v1/resource-slots/", BookingReconciliationSuite::handleResource);
      server.start();
      return server;
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot start Resource stub", exception);
    }
  }

  private static void handleResource(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    if (RESOURCE_UNAVAILABLE.get()) {
      respond(exchange, 503, "");
      return;
    }
    if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/releases")) {
      RELEASE_CALLS.incrementAndGet();
      RELEASE_VISIBLE.set(true);
      if (SLOW_RELEASE.get()) {
        try {
          Thread.sleep(750);
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
        }
      }
      respond(exchange, 201, "");
      return;
    }
    if ("GET".equals(exchange.getRequestMethod())) {
      String operationId = path.substring(path.lastIndexOf('/') + 1);
      boolean release = operationId.startsWith("release:");
      boolean visible = release ? RELEASE_VISIBLE.get() : ALLOCATION_VISIBLE.get();
      if (visible) {
        String type = release ? "RELEASE" : "ALLOCATE";
        respond(
            exchange,
            200,
            JSON.createObjectNode()
                .put("operationId", operationId)
                .put("operationType", type)
                .put("quantity", CONFLICTING_OPERATION.get() ? 2 : 1)
                .toString());
      } else {
        respond(exchange, 404, "");
      }
      return;
    }
    respond(exchange, 404, "");
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
