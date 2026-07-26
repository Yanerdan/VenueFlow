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
                                headers ->
                                    headers.set(
                                        "X-User-Id", authentication.getToken().getSubject())))
                    .build())
        .defaultIfEmpty(cleaned)
        .flatMap(chain::filter);
  }

  @Override
  public int getOrder() {
    return -1;
  }
}
