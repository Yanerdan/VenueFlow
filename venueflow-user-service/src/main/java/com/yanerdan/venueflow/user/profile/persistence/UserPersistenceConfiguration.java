package com.yanerdan.venueflow.user.profile.persistence;

import java.time.Clock;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@MapperScan(basePackageClasses = UserProfileMapper.class)
public class UserPersistenceConfiguration {

  @Bean
  Clock userClock() {
    return Clock.systemUTC();
  }
}
