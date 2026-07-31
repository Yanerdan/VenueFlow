package com.yanerdan.venueflow.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class GatewayRequestBoundaryFilterTest {

  @Test
  void oversizedRequestReleasesEveryInboundBufferWithoutRouting() {
    PooledDataBuffer first = allocatedBuffer();
    PooledDataBuffer second = allocatedBuffer();
    PooledDataBuffer third = allocatedBuffer();
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/auth/login")
                .header(HttpHeaders.CONTENT_LENGTH, "1025")
                .body(Flux.just(first, second, third)));
    AtomicBoolean routed = new AtomicBoolean();

    new GatewayRequestBoundaryFilter(1024)
        .filter(
            exchange,
            ignored -> {
              routed.set(true);
              return Mono.empty();
            })
        .block();

    assertThat(routed).isFalse();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
    verify(first).release();
    verify(second).release();
    verify(third).release();
  }

  private static PooledDataBuffer allocatedBuffer() {
    PooledDataBuffer buffer = mock(PooledDataBuffer.class);
    when(buffer.isAllocated()).thenReturn(true);
    when(buffer.release()).thenReturn(true);
    return buffer;
  }
}
