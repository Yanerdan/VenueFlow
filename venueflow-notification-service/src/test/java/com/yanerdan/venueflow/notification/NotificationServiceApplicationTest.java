package com.yanerdan.venueflow.notification;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NotificationServiceApplicationTest {

  @Autowired private ApplicationContext applicationContext;

  @Autowired private Environment environment;

  @Test
  void startsWithDefaultSkeletonProfile() {
    assertThat(environment.getProperty("spring.application.name"))
        .isEqualTo("venueflow-notification-service");

    assertThat(environment.matchesProfiles("skeleton")).isTrue();

    assertThat(environment.getProperty("server.port")).isEqualTo("8085");
  }

  @Test
  void createsNoInfrastructureBeansOrClients() {
    assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();

    ClassLoader classLoader = getClass().getClassLoader();

    assertThat(
            ClassUtils.isPresent(
                "org.springframework.amqp.rabbit.connection.ConnectionFactory", classLoader))
        .isFalse();

    assertThat(
            ClassUtils.isPresent(
                "org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer",
                classLoader))
        .isFalse();

    assertThat(
            ClassUtils.isPresent("org.springframework.mail.javamail.JavaMailSender", classLoader))
        .isFalse();
  }
}
