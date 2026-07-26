package com.yanerdan.venueflow.booking.checkin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yanerdan.venueflow.booking.collaboration.UserEligibilityClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("persistence")
class BookingCheckInSuite {
  private static final AtomicBoolean FUTURE_SLOT = new AtomicBoolean();
  private static final HttpServer RESOURCE = startResourceStub();

  @Container
  private static final MySQLContainer MYSQL =
      new MySQLContainer("mysql:8.4.10")
          .withDatabaseName("venueflow_booking")
          .withUsername("venueflow_booking_app")
          .withPassword("booking-test-password");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @MockitoBean private UserEligibilityClient userClient;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("venueflow.collaborators.user-base-url", () -> "http://127.0.0.1:1");
    registry.add(
        "venueflow.collaborators.resource-base-url",
        () -> "http://127.0.0.1:" + RESOURCE.getAddress().getPort());
    registry.add("venueflow.booking.check-in-early-window", () -> "PT30M");
    registry.add("venueflow.booking.check-in-late-window", () -> "PT30M");
  }

  @BeforeEach
  void clean() {
    FUTURE_SLOT.set(false);
    jdbc.update("DELETE FROM repair_action");
    jdbc.update("DELETE FROM reconciliation_issue");
    jdbc.update("DELETE FROM reconciliation_run");
    jdbc.update("DELETE FROM booking_reconciliation_intent");
    jdbc.update("DELETE FROM booking_outbox_event");
    jdbc.update("DELETE FROM booking_status_log");
    jdbc.update("DELETE FROM booking_idempotency");
    jdbc.update("DELETE FROM booking_reservation");
    org.mockito.Mockito.when(userClient.isBookingPermitted(1L)).thenReturn(true);
  }

  @AfterAll
  static void stopResourceStub() {
    RESOURCE.stop(0);
  }

  @Test
  void migratesCompletesAtomicallyAndReplays() throws Exception {
    String bookingNo = createAndConfirm();

    String first =
        mockMvc
            .perform(post("/api/v1/bookings/{bookingNo}/check-in", bookingNo))
            .andExpectAll(
                status().isOk(),
                jsonPath("$.data.status").value("COMPLETED"),
                jsonPath("$.data.completedAt").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    String completedAt = JsonPath.read(first, "$.data.completedAt");

    mockMvc
        .perform(post("/api/v1/bookings/{bookingNo}/check-in", bookingNo))
        .andExpectAll(
            status().isOk(),
            jsonPath("$.data.status").value("COMPLETED"),
            jsonPath("$.data.completedAt").value(completedAt));

    assertThat(
            jdbc.queryForList(
                "SELECT event_type FROM booking_outbox_event ORDER BY id", String.class))
        .containsExactly("BOOKING_RESERVATION_CONFIRMED", "BOOKING_RESERVATION_COMPLETED");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM booking_status_log", Integer.class))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '005'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void rejectsCheckInOutsideWindowWithoutCompletionEvent() throws Exception {
    String bookingNo = createAndConfirm();
    FUTURE_SLOT.set(true);

    mockMvc
        .perform(post("/api/v1/bookings/{bookingNo}/check-in", bookingNo))
        .andExpectAll(
            status().isConflict(), jsonPath("$.code").value("BOOKING_CHECK_IN_WINDOW_INVALID"));

    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM booking_reservation WHERE booking_no = ?",
                String.class,
                bookingNo))
        .isEqualTo("CONFIRMED");
    assertThat(
            jdbc.queryForList(
                "SELECT event_type FROM booking_outbox_event ORDER BY id", String.class))
        .containsExactly("BOOKING_RESERVATION_CONFIRMED");
  }

  private String createAndConfirm() throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/v1/bookings")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType("application/json")
                    .content("{\"userId\":1,\"slotId\":2,\"quantity\":1}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    String bookingNo = JsonPath.read(response, "$.data.bookingNo");
    mockMvc
        .perform(post("/api/v1/bookings/{bookingNo}/confirmation", bookingNo))
        .andExpect(status().isOk());
    return bookingNo;
  }

  private static HttpServer startResourceStub() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/api/v1/resource-slots/2", BookingCheckInSuite::handleResource);
      server.start();
      return server;
    } catch (IOException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private static void handleResource(HttpExchange exchange) throws IOException {
    if ("GET".equals(exchange.getRequestMethod())) {
      Instant now = Instant.now();
      Instant start = FUTURE_SLOT.get() ? now.plusSeconds(7200) : now.minusSeconds(60);
      Instant end = start.plusSeconds(3600);
      respond(
          exchange,
          200,
          """
          {"id":2,"resourceId":1,"startAt":"%s","endAt":"%s","status":"OPEN","version":0}
          """
              .formatted(start, end));
      return;
    }
    if ("POST".equals(exchange.getRequestMethod())
        && exchange.getRequestURI().getPath().endsWith("/allocations")) {
      respond(exchange, 200, "{}");
      return;
    }
    respond(exchange, 404, "{}");
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
