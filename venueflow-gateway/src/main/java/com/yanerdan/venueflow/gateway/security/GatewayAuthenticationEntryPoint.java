package com.yanerdan.venueflow.gateway.security;

import com.yanerdan.venueflow.gateway.web.GatewayTraceWebFilter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Profile("gateway")
public class GatewayAuthenticationEntryPoint
    implements org.springframework.security.web.server.ServerAuthenticationEntryPoint {

  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
    return write(
        exchange, HttpStatus.UNAUTHORIZED, "GATEWAY_UNAUTHORIZED", "Authentication required");
  }

  public Mono<Void> accessDenied(
      ServerWebExchange exchange,
      org.springframework.security.access.AccessDeniedException exception) {
    return write(exchange, HttpStatus.FORBIDDEN, "GATEWAY_FORBIDDEN", "Access denied");
  }

  private static Mono<Void> write(
      ServerWebExchange exchange, HttpStatus status, String code, String message) {
    if (exchange.getResponse().isCommitted()) {
      return Mono.empty();
    }
    String traceId = GatewayTraceWebFilter.traceId(exchange);
    String body =
        "{\"code\":\""
            + code
            + "\",\"message\":\""
            + message
            + "\",\"details\":[],\"traceId\":\""
            + traceId
            + "\",\"timestamp\":\""
            + Instant.now()
            + "\"}";
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    exchange.getResponse().getHeaders().setContentLength(bytes.length);
    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }
}
