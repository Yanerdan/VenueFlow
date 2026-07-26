package com.yanerdan.venueflow.booking.expiration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yanerdan.venueflow.booking.application.BookingErrorCode;
import com.yanerdan.venueflow.booking.application.BookingException;
import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient;
import com.yanerdan.venueflow.booking.collaboration.ResourceCapacityClient.ResourceOperation;
import com.yanerdan.venueflow.booking.collaboration.UserEligibilityClient;
import com.yanerdan.venueflow.booking.expiration.application.ExpirationService;
import com.yanerdan.venueflow.booking.expiration.persistence.ExpirationRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
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
@ActiveProfiles({"persistence", "expiration"})
class BookingExpirationSuite {
  @Container
  private static final MySQLContainer MYSQL =
      new MySQLContainer("mysql:8.4.10")
          .withDatabaseName("venueflow_booking")
          .withUsername("venueflow_booking_app")
          .withPassword("booking-test-password");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ExpirationService expiration;
  @Autowired private ExpirationRepository expirationRepository;
  @MockitoBean private UserEligibilityClient userClient;
  @MockitoBean private ResourceCapacityClient resourceClient;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("venueflow.collaborators.user-base-url", () -> "http://127.0.0.1:1");
    registry.add("venueflow.collaborators.resource-base-url", () -> "http://127.0.0.1:1");
    registry.add("venueflow.booking.confirmation-window", () -> "PT30S");
    registry.add("venueflow.booking.expiration.enabled", () -> false);
    registry.add("venueflow.booking.expiration.confirmation-window", () -> "PT30S");
    registry.add("venueflow.booking.expiration.batch-size", () -> 10);
    registry.add("venueflow.booking.expiration.lease-duration", () -> "PT5S");
    registry.add("venueflow.booking.expiration.scan-delay", () -> "PT1S");
    registry.add("venueflow.booking.expiration.max-attempts", () -> 3);
    registry.add("venueflow.booking.expiration.initial-backoff", () -> "PT1S");
    registry.add("venueflow.booking.expiration.max-backoff", () -> "PT5S");
    registry.add("venueflow.booking.expiration.operation-lookup-timeout", () -> "PT1S");
    registry.add("venueflow.booking.expiration.connect-timeout", () -> "PT1S");
    registry.add("venueflow.booking.expiration.request-timeout", () -> "PT2S");
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
    when(userClient.isBookingPermitted(1L)).thenReturn(true);
  }

  @Test
  void migratesAndConfirmsPendingBeforeDeadline() throws Exception {
    String bookingNo = createPending();

    mockMvc
        .perform(post("/api/v1/bookings/{bookingNo}/confirmation", bookingNo))
        .andExpectAll(status().isOk(), jsonPath("$.data.status").value("CONFIRMED"));

    assertThat(eventTypes()).containsExactly("BOOKING_RESERVATION_CONFIRMED");
    assertThat(statusLogCount()).isEqualTo(2);
  }

  @Test
  void claimsOnceAndReclaimsExpiredLease() throws Exception {
    createDue();
    LocalDateTime now = LocalDateTime.now();
    assertThat(expirationRepository.claim(now, 1, "worker-one", now.plusSeconds(5)).reservations())
        .hasSize(1);
    assertThat(expirationRepository.claim(now, 1, "worker-two", now.plusSeconds(5)).reservations())
        .isEmpty();
    jdbc.update(
        "UPDATE booking_reservation SET timeout_lease_expires_at = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND");
    assertThat(
            expirationRepository
                .claim(LocalDateTime.now(), 1, "worker-two", LocalDateTime.now().plusSeconds(5))
                .leaseReclaimed())
        .isEqualTo(1);
  }

  @Test
  void expiresOnlyAfterReleaseProof() throws Exception {
    String bookingNo = createDue();
    String releaseId = releaseId();
    when(resourceClient.findOperation(2L, releaseId)).thenReturn(Optional.empty());

    assertThat(expiration.runOnce("TEST", "expired").expired()).isEqualTo(1);

    assertThat(bookingStatus(bookingNo)).isEqualTo("EXPIRED");
    assertThat(eventTypes()).containsExactly("BOOKING_RESERVATION_EXPIRED");
    verify(resourceClient).release(2L, releaseId, 1);
  }

  @Test
  void lostReleaseResponseIsProvenByLookup() throws Exception {
    String bookingNo = createDue();
    String releaseId = releaseId();
    when(resourceClient.findOperation(2L, releaseId))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(new ResourceOperation(releaseId, "RELEASE", 1)));
    doThrow(new BookingException(BookingErrorCode.BOOKING_DOWNSTREAM_UNAVAILABLE, "response lost"))
        .when(resourceClient)
        .release(2L, releaseId, 1);

    assertThat(expiration.runOnce("TEST", "lost-response").expired()).isEqualTo(1);
    assertThat(bookingStatus(bookingNo)).isEqualTo("EXPIRED");
    assertThat(eventTypes()).containsExactly("BOOKING_RESERVATION_EXPIRED");
  }

  private String createDue() throws Exception {
    String bookingNo = createPending();
    jdbc.update(
        """
        UPDATE booking_reservation
        SET expire_at = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND,
            timeout_next_check_at = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND
        WHERE booking_no = ?
        """,
        bookingNo);
    return bookingNo;
  }

  private String createPending() throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/v1/bookings")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType("application/json")
                    .content("{\"userId\":1,\"slotId\":2,\"quantity\":1}"))
            .andExpectAll(
                status().isCreated(),
                jsonPath("$.data.status").value("PENDING_CONFIRMATION"),
                jsonPath("$.data.expireAt").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    return JsonPath.read(response, "$.data.bookingNo");
  }

  private String releaseId() {
    return jdbc.queryForObject(
        "SELECT release_operation_id FROM booking_reservation", String.class);
  }

  private String bookingStatus(String bookingNo) {
    return jdbc.queryForObject(
        "SELECT status FROM booking_reservation WHERE booking_no = ?", String.class, bookingNo);
  }

  private int statusLogCount() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM booking_status_log", Integer.class);
  }

  private java.util.List<String> eventTypes() {
    return jdbc.queryForList(
        "SELECT event_type FROM booking_outbox_event ORDER BY id", String.class);
  }
}
