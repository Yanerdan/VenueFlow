package com.yanerdan.venueflow.gateway.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("gateway")
public class GatewayRouteConfiguration {

  @Bean
  RouteLocator venueFlowRoutes(
      RouteLocatorBuilder builder,
      @Value("${venueflow.gateway.auth-uri}") String authUri,
      @Value("${venueflow.gateway.user-uri}") String userUri,
      @Value("${venueflow.gateway.resource-uri}") String resourceUri,
      @Value("${venueflow.gateway.booking-uri}") String bookingUri) {
    return builder
        .routes()
        .route("auth", route -> route.path("/api/v1/auth/**").uri(baseUri(authUri)))
        .route("users", route -> route.path("/api/v1/users/**").uri(baseUri(userUri)))
        .route("resources", route -> route.path("/api/v1/resources/**").uri(baseUri(resourceUri)))
        .route("bookings", route -> route.path("/api/v1/bookings/**").uri(baseUri(bookingUri)))
        .build();
  }

  private static URI baseUri(String value) {
    URI uri = URI.create(value);
    if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()))) {
      throw new IllegalArgumentException("Gateway route URI must be a plain HTTP(S) origin");
    }
    return uri;
  }
}
