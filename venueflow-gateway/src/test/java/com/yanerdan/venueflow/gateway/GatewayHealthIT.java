package com.yanerdan.venueflow.gateway;

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
class GatewayHealthIT {

  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Autowired private Environment environment;

  @Test
  void probesAreHealthy() throws IOException, InterruptedException {
    assertThat(get("/actuator/health/liveness").statusCode()).isEqualTo(200);
    assertThat(get("/actuator/health/readiness").statusCode()).isEqualTo(200);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/actuator", "/actuator/env", "/actuator/metrics"})
  void sensitiveManagementEndpointsAreNotExposed(String path)
      throws IOException, InterruptedException {
    assertThat(get(path).statusCode()).isEqualTo(404);
  }

  private HttpResponse<String> get(String path) throws IOException, InterruptedException {
    int port = environment.getRequiredProperty("local.server.port", Integer.class);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
