package com.yanerdan.venueflow.auth.security;

import com.yanerdan.venueflow.auth.application.OidcProviderProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OidcProviderProperties.class)
@Profile("persistence")
public class FederatedIdentityConfiguration {

  @Bean
  RestClient.Builder oidcRestClientBuilder() {
    return RestClient.builder();
  }
}
