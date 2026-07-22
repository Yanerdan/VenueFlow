package com.yanerdan.venueflow.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserServiceHealthIT {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

  private final BasicJsonTester json = new BasicJsonTester(getClass());

  @LocalServerPort private int port;

  @Test
  void livenessProbeReportsUpWithoutDetails() throws IOException, InterruptedException {
    assertHealthyProbe("/actuator/health/liveness");
  }

  @Test
  void readinessProbeReportsUpWithoutDetails() throws IOException, InterruptedException {
    assertHealthyProbe("/actuator/health/readiness");
  }

  @Test
  void rootHealthReportsUpAndListsOnlyProbeGroups() throws IOException, InterruptedException {
    HttpResponse<String> response = get("/actuator/health");

    assertThat(response.statusCode()).isEqualTo(200);

    Map<String, Object> payload = JsonPath.read(response.body(), "$");

    List<String> groups = JsonPath.read(response.body(), "$.groups");

    assertThat(payload).containsOnlyKeys("status", "groups");

    assertThat(payload.get("status")).isEqualTo("UP");

    assertThat(groups).containsExactlyInAnyOrder("liveness", "readiness");

    assertThat(response.body()).doesNotContain("\"components\"", "\"details\"", "\"diskSpace\"");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/actuator",
        "/actuator/env",
        "/actuator/configprops",
        "/actuator/loggers",
        "/actuator/mappings",
        "/actuator/metrics",
        "/actuator/beans",
        "/actuator/conditions",
        "/actuator/info",
        "/actuator/heapdump",
        "/actuator/threaddump",
        "/actuator/scheduledtasks"
      })
  void sensitiveManagementEndpointIsNotExposed(String path)
      throws IOException, InterruptedException {
    HttpResponse<String> response = get(path);

    assertThat(response.statusCode()).as("HTTP status for %s", path).isEqualTo(404);
  }

  private void assertHealthyProbe(String path) throws IOException, InterruptedException {
    HttpResponse<String> response = get(path);

    assertThat(response.statusCode()).as("HTTP status for %s", path).isEqualTo(200);

    assertThat(json.from(response.body()))
        .isStrictlyEqualToJson(
            """
                {
                  "status": "UP"
                }
                """);
  }

  private HttpResponse<String> get(String path) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + path))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();

    return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
