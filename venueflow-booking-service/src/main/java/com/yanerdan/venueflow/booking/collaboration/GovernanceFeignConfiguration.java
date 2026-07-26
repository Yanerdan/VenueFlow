package com.yanerdan.venueflow.booking.collaboration;

import feign.RequestInterceptor;
import feign.Retryer;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("governance")
@EnableFeignClients(clients = {FeignUserEligibilityApi.class, FeignResourceCapacityApi.class})
class GovernanceFeignConfiguration {

  @Bean
  Retryer governanceFeignRetryer() {
    return Retryer.NEVER_RETRY;
  }

  @Bean
  RequestInterceptor traceRequestInterceptor() {
    return template -> {
      String traceId = MDC.get("traceId");
      if (isCanonicalUuid(traceId)) {
        template.header("X-Trace-Id", traceId);
      }
      template.removeHeader("X-User-Id");
      template.removeHeader("X-Role");
    };
  }

  private static boolean isCanonicalUuid(String value) {
    if (value == null || value.length() != 36) {
      return false;
    }
    try {
      return UUID.fromString(value).toString().equals(value);
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }
}
