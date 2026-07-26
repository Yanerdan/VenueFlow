package com.yanerdan.venueflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.web.server.SecurityWebFilterChain;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationTest {

  @Autowired private ApplicationContext applicationContext;
  @Autowired private Environment environment;

  @Test
  void startsWithConnectionFreeSkeletonProfile() {
    assertThat(environment.getProperty("spring.application.name")).isEqualTo("venueflow-gateway");
    assertThat(environment.matchesProfiles("skeleton")).isTrue();
    assertThat(applicationContext.getBeansOfType(SecurityWebFilterChain.class)).isEmpty();
  }
}
