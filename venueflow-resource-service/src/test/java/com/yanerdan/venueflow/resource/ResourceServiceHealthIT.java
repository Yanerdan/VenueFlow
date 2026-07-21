package com.yanerdan.venueflow.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Timeout(30)
class ResourceServiceHealthIT {

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @LocalServerPort private int port;

  @Test
  void exposesOnlyHealthyLivenessAndReadinessProbes() throws Exception {
    for (String group : List.of("liveness", "readiness")) {
      HttpResponse<String> response = get("/actuator/health/" + group);

      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).contains("\"status\":\"UP\"");
      assertThat(response.body()).doesNotContain("components");
    }
  }

  @Test
  void doesNotExposeSensitiveManagementEndpoints() throws Exception {
    for (String endpoint : List.of("env", "configprops", "loggers", "mappings", "metrics")) {
      HttpResponse<String> response = get("/actuator/" + endpoint);

      assertThat(response.statusCode()).as(endpoint).isEqualTo(404);
      assertThat(response.body()).as(endpoint).doesNotContain("propertySources", "contexts");
    }
  }

  private HttpResponse<String> get(String path) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
