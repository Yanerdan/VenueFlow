package com.yanerdan.venueflow.booking.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient;
import com.yanerdan.venueflow.booking.collaboration.UserEligibilityClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("persistence")
class BookingReservationMysqlSuite {
  private static final String KEY = "f4f4266a-b145-44f4-a375-0d59450f5147";

  @Container
  private static final MySQLContainer MYSQL =
      new MySQLContainer("mysql:8.4.10")
          .withDatabaseName("venueflow_booking")
          .withUsername("venueflow_booking_app")
          .withPassword("booking-test-password");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @MockitoBean private UserEligibilityClient userClient;
  @MockitoBean private ResourceCapacityClient resourceClient;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("venueflow.collaborators.user-base-url", () -> "http://127.0.0.1:1");
    registry.add("venueflow.collaborators.resource-base-url", () -> "http://127.0.0.1:1");
  }

  @BeforeEach
  void clean() {
    jdbcTemplate.update("DELETE FROM repair_action");
    jdbcTemplate.update("DELETE FROM reconciliation_issue");
    jdbcTemplate.update("DELETE FROM reconciliation_run");
    jdbcTemplate.update("DELETE FROM booking_reconciliation_intent");
    jdbcTemplate.update("DELETE FROM booking_outbox_event");
    jdbcTemplate.update("DELETE FROM booking_status_log");
    jdbcTemplate.update("DELETE FROM booking_idempotency");
    jdbcTemplate.update("DELETE FROM booking_reservation");
    when(userClient.isBookingPermitted(1L)).thenReturn(true);
  }

  @Test
  void migratesCreatesReplaysRetrievesAndCancels() throws Exception {
    Integer migrations =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM flyway_schema_history
            WHERE script IN (
              'V001__init_booking_reservations.sql',
              'V002__add_booking_outbox.sql',
              'V003__add_booking_reconciliation.sql',
              'V004__add_booking_timeout_expiration.sql'
            ) AND success = 1
            """,
            Integer.class);
    assertThat(migrations).isEqualTo(4);

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/bookings")
                    .header("Idempotency-Key", KEY)
                    .contentType("application/json")
                    .content("{\"userId\":1,\"slotId\":2,\"quantity\":1}"))
            .andExpectAll(
                status().isCreated(),
                jsonPath("$.data.status").value("PENDING_CONFIRMATION"),
                jsonPath("$.data.expireAt").isNotEmpty())
            .andReturn();
    String bookingNo =
        JsonPath.read(
            created.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.bookingNo");

    mockMvc
        .perform(
            post("/api/v1/bookings")
                .header("Idempotency-Key", KEY)
                .contentType("application/json")
                .content("{\"userId\":1,\"slotId\":2,\"quantity\":1}"))
        .andExpectAll(status().isOk(), jsonPath("$.data.bookingNo").value(bookingNo));
    mockMvc
        .perform(get("/api/v1/bookings/{bookingNo}", bookingNo))
        .andExpectAll(status().isOk(), jsonPath("$.data.status").value("PENDING_CONFIRMATION"));
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM booking_outbox_event", Integer.class))
        .isZero();
    mockMvc
        .perform(
            post("/api/v1/bookings/{bookingNo}/confirmation", bookingNo)
                .header("X-Role", "SYSTEM_ADMIN"))
        .andExpectAll(status().isOk(), jsonPath("$.data.status").value("CONFIRMED"));
    mockMvc
        .perform(post("/api/v1/bookings/{bookingNo}/cancellation", bookingNo))
        .andExpectAll(status().isOk(), jsonPath("$.data.status").value("CANCELLED"));

    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM booking_reservation", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForList(
                "SELECT event_type FROM booking_outbox_event ORDER BY id", String.class))
        .containsExactly("BOOKING_RESERVATION_CONFIRMED", "BOOKING_RESERVATION_CANCELLED");
    assertThat(
            jdbcTemplate.queryForList(
                "SELECT state FROM booking_reconciliation_intent ORDER BY id", String.class))
        .containsExactly("RESOLVED", "RESOLVED");
    verify(resourceClient)
        .allocate(
            org.mockito.ArgumentMatchers.eq(2L),
            org.mockito.ArgumentMatchers.startsWith("allocate:"),
            org.mockito.ArgumentMatchers.eq(1));
  }

  @Test
  void concurrentIdenticalCreatesHaveOneExecutorAndOneReservation() throws Exception {
    CountDownLatch ownerAtUser = new CountDownLatch(1);
    CountDownLatch continueOwner = new CountDownLatch(1);
    when(userClient.isBookingPermitted(1L))
        .thenAnswer(
            ignored -> {
              ownerAtUser.countDown();
              return continueOwner.await(10, TimeUnit.SECONDS);
            });

    try (ExecutorService requests = Executors.newFixedThreadPool(2)) {
      Future<MvcResult> owner = requests.submit(this::create);
      assertThat(ownerAtUser.await(10, TimeUnit.SECONDS)).isTrue();
      Future<MvcResult> concurrent = requests.submit(this::create);

      assertThat(concurrent.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(409);
      continueOwner.countDown();
      MvcResult created = owner.get(10, TimeUnit.SECONDS);
      assertThat(created.getResponse().getStatus()).isEqualTo(201);
      String bookingNo =
          JsonPath.read(
              created.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.bookingNo");

      assertThat(create().getResponse().getStatus()).isEqualTo(200);
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM booking_reservation", Integer.class))
          .isEqualTo(1);
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT booking_no FROM booking_reservation", String.class))
          .isEqualTo(bookingNo);
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM booking_outbox_event", Integer.class))
          .isZero();
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM booking_reconciliation_intent", Integer.class))
          .isEqualTo(1);
      verify(userClient).isBookingPermitted(1L);
      verify(resourceClient).allocate(2L, "allocate:" + requestId(), 1);
    } finally {
      continueOwner.countDown();
    }
  }

  private MvcResult create() throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/bookings")
                .header("Idempotency-Key", KEY)
                .contentType("application/json")
                .content("{\"userId\":1,\"slotId\":2,\"quantity\":1}"))
        .andReturn();
  }

  private String requestId() {
    return jdbcTemplate.queryForObject(
        "SELECT request_id FROM booking_idempotency WHERE user_id = 1", String.class);
  }
}
