package com.yanerdan.venueflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthServiceHealthIT {

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Autowired private Environment environment;

  @Test
  void livenessProbeIsUp() throws IOException, InterruptedException {
    assertHealthy("/actuator/health/liveness");
  }

  @Test
  void readinessProbeIsUp() throws IOException, InterruptedException {
    assertHealthy("/actuator/health/readiness");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/actuator",
        "/actuator/env",
        "/actuator/configprops",
        "/actuator/loggers",
        "/actuator/mappings",
        "/actuator/metrics"
      })
  void sensitiveManagementEndpointIsNotExposed(String path)
      throws IOException, InterruptedException {
    assertThat(get(path).statusCode()).as(path).isEqualTo(404);
  }

  private void assertHealthy(String path) throws IOException, InterruptedException {
    HttpResponse<String> response = get(path);
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body())
        .contains("\"status\":\"UP\"")
        .doesNotContain("\"components\"", "\"details\"");
  }

  private HttpResponse<String> get(String path) throws IOException, InterruptedException {
    Integer port = environment.getRequiredProperty("local.server.port", Integer.class);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
