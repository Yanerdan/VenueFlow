package com.yanerdan.venueflow.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.yanerdan.venueflow.user.profile.domain.UserProfileRepository;
import com.yanerdan.venueflow.user.profile.persistence.UserProfileMapper;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootTest
class UserServiceApplicationTest {

  private final ApplicationContext applicationContext;
  private final Environment environment;

  @Autowired
  UserServiceApplicationTest(ApplicationContext applicationContext, Environment environment) {
    this.applicationContext = applicationContext;
    this.environment = environment;
  }

  @Test
  void startsWithDefaultSkeletonProfileWithoutExternalInfrastructure() {
    assertThat(applicationContext).isNotNull();

    assertThat(environment.getProperty("spring.application.name"))
        .isEqualTo("venueflow-user-service");

    assertThat(environment.matchesProfiles("skeleton")).isTrue();

    assertThat(environment.getDefaultProfiles()).contains("skeleton");

    assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8082);

    assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();

    assertThat(applicationContext.getBeansOfType(UserProfileRepository.class)).isEmpty();

    assertThat(applicationContext.getBeansOfType(UserProfileMapper.class)).isEmpty();
  }
}
