package com.yanerdan.venueflow.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.yanerdan.venueflow.booking.expiration.application.ExpirationScheduler;
import com.yanerdan.venueflow.booking.expiration.application.ExpirationService;
import com.yanerdan.venueflow.booking.reconciliation.application.ReconciliationScheduler;
import com.yanerdan.venueflow.booking.reconciliation.application.ReconciliationService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootTest
class BookingServiceApplicationTest {

  private final ApplicationContext applicationContext;
  private final Environment environment;

  @Autowired
  BookingServiceApplicationTest(ApplicationContext applicationContext, Environment environment) {
    this.applicationContext = applicationContext;
    this.environment = environment;
  }

  @Test
  void startsWithDefaultSkeletonProfileWithoutExternalInfrastructure() {
    assertThat(applicationContext).isNotNull();
    assertThat(environment.getProperty("spring.application.name"))
        .isEqualTo("venueflow-booking-service");
    assertThat(environment.matchesProfiles("skeleton")).isTrue();
    assertThat(environment.getDefaultProfiles()).contains("skeleton");
    assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8084);
    assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(ConnectionFactory.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(RabbitTemplate.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(ReconciliationService.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(ReconciliationScheduler.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(ExpirationService.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(ExpirationScheduler.class)).isEmpty();
  }

  @Test
  void reconciliationMigrationContainsOnlyBookingOwnedRecoveryTables() throws IOException {
    String migration =
        Files.readString(
            Path.of(
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V003__add_booking_reconciliation.sql"));

    assertThat(migration)
        .contains(
            "CREATE TABLE booking_reconciliation_intent",
            "CREATE TABLE reconciliation_run",
            "CREATE TABLE reconciliation_issue",
            "CREATE TABLE repair_action")
        .doesNotContain(
            "CREATE TABLE resource",
            "ALTER TABLE resource",
            "response_body",
            "request_body",
            "stack_trace",
            "password",
            "jdbc_url");
  }
}
