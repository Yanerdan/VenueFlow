package com.yanerdan.venueflow.booking;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
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
  }
}
