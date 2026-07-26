package com.yanerdan.venueflow.gateway.config;

import com.yanerdan.venueflow.gateway.web.GatewayTraceWebFilter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@Profile("gateway")
public class GatewayCorsConfiguration {

  @Bean
  CorsWebFilter gatewayCorsWebFilter(
      @Value("${venueflow.gateway.allowed-origins}") String configuredOrigins) {
    List<String> origins =
        Arrays.stream(configuredOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .map(GatewayCorsConfiguration::explicitOrigin)
            .toList();
    if (origins.isEmpty()) {
      throw new IllegalArgumentException("At least one explicit Gateway CORS origin is required");
    }

    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(origins);
    cors.setAllowCredentials(true);
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(
        List.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            "Idempotency-Key",
            GatewayTraceWebFilter.TRACE_HEADER));
    cors.setExposedHeaders(List.of(GatewayTraceWebFilter.TRACE_HEADER));
    cors.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return new CorsWebFilter(source);
  }

  private static String explicitOrigin(String value) {
    URI uri = URI.create(value);
    if ("*".equals(value)
        || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null
        || (uri.getPath() != null && !uri.getPath().isBlank())) {
      throw new IllegalArgumentException("Gateway CORS origins must be explicit HTTP(S) origins");
    }
    return value;
  }
}
