package com.yanerdan.venueflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AuthServiceApplicationTest {

  @Autowired private ApplicationContext applicationContext;

  @Autowired private Environment environment;

  @Test
  void startsWithDefaultSkeletonProfile() {
    assertThat(environment.getProperty("spring.application.name"))
        .isEqualTo("venueflow-auth-service");
    assertThat(environment.matchesProfiles("skeleton")).isTrue();
    assertThat(environment.getProperty("server.port")).isEqualTo("8081");
  }

  @Test
  void createsNoInfrastructureSecurityOrCollaboratorBeans() {
    assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();

    ClassLoader classLoader = getClass().getClassLoader();
    assertThat(
            ClassUtils.isPresent("org.springframework.security.core.Authentication", classLoader))
        .isFalse();
    assertThat(ClassUtils.isPresent("org.springframework.jdbc.core.JdbcTemplate", classLoader))
        .isFalse();
    assertThat(ClassUtils.isPresent("org.springframework.cloud.openfeign.FeignClient", classLoader))
        .isFalse();
  }
}
