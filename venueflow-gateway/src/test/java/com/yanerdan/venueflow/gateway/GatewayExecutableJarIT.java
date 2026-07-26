package com.yanerdan.venueflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class GatewayExecutableJarIT {

  @Test
  void executableJarStartsWithoutInfrastructure() throws Exception {
    Path jar;
    try (var paths = Files.list(Path.of("target"))) {
      jar =
          paths
              .filter(path -> path.getFileName().toString().matches("venueflow-gateway-.+\\.jar"))
              .filter(path -> !path.getFileName().toString().endsWith(".original"))
              .findFirst()
              .orElseThrow();
    }
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    Path java = Path.of(System.getProperty("java.home"), "bin", "java.exe");
    ProcessBuilder builder =
        new ProcessBuilder(java.toString(), "-jar", jar.toAbsolutePath().toString());
    builder.environment().put("SERVER_PORT", Integer.toString(port));
    builder.environment().remove("SPRING_PROFILES_ACTIVE");
    builder.redirectErrorStream(true);
    builder.redirectOutput(Path.of("target", "gateway-jar-it.log").toFile());
    Process process = builder.start();
    try {
      HttpResponse<String> response = waitForHealth(process, port);
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).contains("\"status\":\"UP\"");
      assertThat(get(port, "/actuator/env").statusCode()).isEqualTo(404);
    } finally {
      process.destroy();
      if (!process.waitFor(5, TimeUnit.SECONDS)) {
        process.destroyForcibly();
      }
    }
  }

  private static HttpResponse<String> waitForHealth(Process process, int port)
      throws InterruptedException {
    Instant deadline = Instant.now().plusSeconds(30);
    while (Instant.now().isBefore(deadline)) {
      if (!process.isAlive()) {
        throw new AssertionError("Gateway JAR exited before becoming healthy");
      }
      try {
        HttpResponse<String> response = get(port, "/actuator/health/liveness");
        if (response.statusCode() == 200) {
          return response;
        }
      } catch (IOException ignored) {
        // Startup is still in progress.
      }
      Thread.sleep(250);
    }
    throw new AssertionError("Gateway JAR did not become healthy");
  }

  private static HttpResponse<String> get(int port, String path)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(3))
            .GET()
            .build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }
}
