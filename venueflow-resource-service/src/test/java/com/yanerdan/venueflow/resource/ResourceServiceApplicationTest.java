package com.yanerdan.venueflow.resource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ResourceServiceApplicationTest {

  @Autowired private ApplicationContext applicationContext;

  @Autowired private Environment environment;

  @Test
  void startsStandaloneWithExpectedIdentity() {
    assertThat(applicationContext).isNotNull();
    assertThat(environment.getProperty("spring.application.name"))
        .isEqualTo("venueflow-resource-service");
    assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8083);
    assertThat(environment.getProperty("spring.config.import")).isNull();
    assertThat(environment.getProperty("spring.datasource.url")).isNull();
  }
}
