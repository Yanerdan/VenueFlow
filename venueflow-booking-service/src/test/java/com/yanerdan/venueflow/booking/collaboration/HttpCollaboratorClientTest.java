package com.yanerdan.venueflow.booking.collaboration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpCollaboratorClientTest {
  private HttpServer server;
  private ExecutorService executor;

  @AfterEach
  void stop() {
    if (server != null) server.stop(0);
    if (executor != null) executor.shutdownNow();
  }

  @Test
  void readsTypedEligibilityResponse() throws Exception {
    start();
    server.createContext(
        "/api/v1/users/1/booking-eligibility",
        exchange ->
            respond(
                exchange,
                200,
                """
                {"userId":1,"accountStatus":"ACTIVE","bookingEligibility":"ELIGIBLE",
                 "bookingPermitted":true,"version":0,"updatedAt":"2026-07-27T04:03:56Z"}
                """));
    server.start();

    HttpUserEligibilityClient client =
        new HttpUserEligibilityClient(new ObjectMapper(), baseUrl(), 200, 500);

    assertThat(client.isBookingPermitted(1L)).isTrue();
  }

  @Test
  void resolvesTimedOutAllocationByOperationLookupWithoutResendingWrite() throws Exception {
    start();
    AtomicInteger allocations = new AtomicInteger();
    server.createContext(
        "/api/v1/resource-slots/2/allocations",
        exchange -> {
          allocations.incrementAndGet();
          try {
            Thread.sleep(150);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
          }
          respond(exchange, 201, "{}");
        });
    server.createContext(
        "/api/v1/resource-slots/2/allocation-operations/allocate-request-1",
        exchange ->
            respond(
                exchange,
                200,
                "{\"operationId\":\"allocate-request-1\",\"operationType\":\"ALLOCATE\",\"quantity\":1}"));
    server.start();

    HttpResourceCapacityClient client =
        new HttpResourceCapacityClient(new ObjectMapper(), baseUrl(), 200, 50, 1);
    client.allocate(2L, "allocate-request-1", 1);

    assertThat(allocations).hasValue(1);
  }

  private void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    executor = Executors.newCachedThreadPool();
    server.setExecutor(executor);
  }

  private String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
      throws java.io.IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
