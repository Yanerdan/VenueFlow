package com.yanerdan.venueflow.gateway.web;

import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Profile("gateway")
public class GatewayTraceWebFilter implements WebFilter, Ordered {

  public static final String TRACE_HEADER = "X-Trace-Id";
  private static final String TRACE_ATTRIBUTE = GatewayTraceWebFilter.class.getName() + ".traceId";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String traceId = normalizedTraceId(exchange.getRequest().getHeaders().getFirst(TRACE_HEADER));
    exchange.getAttributes().put(TRACE_ATTRIBUTE, traceId);
    exchange.getResponse().getHeaders().set(TRACE_HEADER, traceId);
    exchange.getResponse().getHeaders().set("X-Content-Type-Options", "nosniff");
    exchange.getResponse().getHeaders().set("X-Frame-Options", "DENY");
    exchange.getResponse().getHeaders().set("Referrer-Policy", "no-referrer");
    exchange.getResponse().getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-store");
    exchange
        .getResponse()
        .beforeCommit(
            () -> {
              exchange.getResponse().getHeaders().set(TRACE_HEADER, traceId);
              return Mono.empty();
            });
    ServerWebExchange traced =
        exchange
            .mutate()
            .request(request -> request.headers(headers -> headers.set(TRACE_HEADER, traceId)))
            .build();
    return chain.filter(traced);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  public static String traceId(ServerWebExchange exchange) {
    Object value = exchange.getAttribute(TRACE_ATTRIBUTE);
    return value instanceof String string ? string : UUID.randomUUID().toString();
  }

  private static String normalizedTraceId(String candidate) {
    if (candidate != null && candidate.length() == 36) {
      try {
        UUID uuid = UUID.fromString(candidate);
        if (uuid.toString().equals(candidate.toLowerCase(Locale.ROOT))) {
          return uuid.toString();
        }
      } catch (IllegalArgumentException ignored) {
        // Replace malformed client trace identifiers.
      }
    }
    return UUID.randomUUID().toString();
  }
}
