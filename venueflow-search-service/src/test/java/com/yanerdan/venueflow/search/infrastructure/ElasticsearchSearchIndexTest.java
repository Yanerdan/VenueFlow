package com.yanerdan.venueflow.search.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yanerdan.venueflow.search.application.ResourceDocument;
import com.yanerdan.venueflow.search.application.ResourceSearchPage;
import com.yanerdan.venueflow.search.application.ResourceSearchQuery;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ElasticsearchSearchIndexTest {

  private HttpServer server;
  private Rest5Client client;
  private ElasticsearchSearchIndex index;
  private final AtomicReference<String> requestTarget = new AtomicReference<>();
  private final AtomicReference<String> requestBody = new AtomicReference<>();

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", this::respond);
    server.start();
    client =
        Rest5Client.builder(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
            .build();
    index =
        new ElasticsearchSearchIndex(
            client,
            new ObjectMapper(),
            "venueflow-resource-read",
            "venueflow-resource-write",
            "venueflow-search-inbox");
  }

  @AfterEach
  void tearDown() throws IOException {
    client.close();
    server.stop(0);
  }

  @Test
  void usesExternalVersionForProjectionWrites() {
    index.upsert(document());

    assertThat(requestTarget.get())
        .contains("/venueflow-resource-write/_doc/7")
        .contains("version=3")
        .contains("version_type=external_gte");
  }

  @Test
  void sendsBoundedTypedSearchAndParsesHits() throws Exception {
    ResourceSearchPage page = index.search(new ResourceSearchQuery("room", 2L, "active", 0, 20));

    JsonNode body = new ObjectMapper().readTree(requestBody.get());
    assertThat(body.path("size").intValue()).isEqualTo(20);
    assertThat(body.toString()).contains("\"categoryId\":2").contains("\"status\":\"ACTIVE\"");
    assertThat(page.totalElements()).isEqualTo(1);
    assertThat(page.items()).containsExactly(document());
  }

  private void respond(HttpExchange exchange) throws IOException {
    requestTarget.set(exchange.getRequestURI().toString());
    requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    String response =
        exchange.getRequestURI().getPath().endsWith("/_search")
            ? """
              {"hits":{"total":{"value":1},"hits":[{"_source":{
                "resourceId":7,"resourceNo":"R-7","categoryId":2,"name":"Room",
                "description":"Description","location":"A","capacity":10,
                "status":"ACTIVE","version":3,"updatedAt":"2026-07-26T12:00:00"
              }}]}}
              """
            : "{}";
    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private static ResourceDocument document() {
    return new ResourceDocument(
        7L, "R-7", 2L, "Room", "Description", "A", 10, "ACTIVE", 3L, "2026-07-26T12:00:00");
  }
}
