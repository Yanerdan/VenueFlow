package com.yanerdan.venueflow.gateway.web;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Profile("gateway")
public class GatewayRequestBoundaryFilter implements GlobalFilter, Ordered {

  private final long maxRequestBytes;

  public GatewayRequestBoundaryFilter(
      @Value("${venueflow.gateway.max-request-bytes}") long maxRequestBytes) {
    this.maxRequestBytes = maxRequestBytes;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (maxRequestBytes < 1 || maxRequestBytes > 10L * 1024 * 1024) {
      return Mono.error(new IllegalStateException("Gateway request limit is outside safe bounds"));
    }
    if (exchange.getRequest().getHeaders().getContentLength() <= maxRequestBytes) {
      return chain.filter(exchange);
    }
    return exchange
        .getRequest()
        .getBody()
        .doOnNext(DataBufferUtils::release)
        .then(Mono.defer(() -> writePayloadTooLarge(exchange)));
  }

  private static Mono<Void> writePayloadTooLarge(ServerWebExchange exchange) {
    byte[] body =
        ("{\"code\":\"GATEWAY_PAYLOAD_TOO_LARGE\",\"message\":\"Request body is too large\","
                + "\"details\":[],\"traceId\":\""
                + GatewayTraceWebFilter.traceId(exchange)
                + "\"}")
            .getBytes(StandardCharsets.UTF_8);
    exchange.getResponse().setStatusCode(HttpStatus.CONTENT_TOO_LARGE);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    exchange.getResponse().getHeaders().setContentLength(body.length);
    return exchange
        .getResponse()
        .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
  }

  @Override
  public int getOrder() {
    return -2;
  }
}
