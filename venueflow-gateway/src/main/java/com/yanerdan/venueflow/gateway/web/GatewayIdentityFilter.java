package com.yanerdan.venueflow.gateway.web;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Profile("gateway")
public class GatewayIdentityFilter implements GlobalFilter, Ordered {
  private static final java.util.Set<String> CAMPUS_ROLES =
      java.util.Set.of("APPLICANT", "APPROVER", "RESOURCE_MANAGER", "SYSTEM_ADMIN");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerWebExchange cleaned =
        exchange
            .mutate()
            .request(
                request ->
                    request.headers(
                        headers -> {
                          headers.remove("X-User-Id");
                          headers.remove("X-Role");
                        }))
            .build();
    return cleaned
        .getPrincipal()
        .ofType(JwtAuthenticationToken.class)
        .filter(JwtAuthenticationToken::isAuthenticated)
        .map(
            authentication ->
                cleaned
                    .mutate()
                    .request(
                        request ->
                            request.headers(
                                headers -> {
                                  String role = authentication.getToken().getClaimAsString("role");
                                  if (!CAMPUS_ROLES.contains(role)) {
                                    role = "APPLICANT";
                                  }
                                  headers.set("X-User-Id", authentication.getToken().getSubject());
                                  headers.set("X-Role", role);
                                }))
                    .build())
        .defaultIfEmpty(cleaned)
        .flatMap(chain::filter);
  }

  @Override
  public int getOrder() {
    return -1;
  }
}
