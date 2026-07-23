package com.yanerdan.venueflow.notification;

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
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class NotificationServiceExecutableJarIT {

  private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30);

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

  @Test
  void executableJarStartsWithServerPortOverride() throws Exception {
    Path jar = findExecutableJar();

    int port = reservePort();

    Path log = Path.of("target", "notification-service-jar-it.log").toAbsolutePath().normalize();

    Files.deleteIfExists(log);

    Process process = startJar(jar, port, log);

    try {
      waitUntilHealthy(process, port, log);

      HttpResponse<String> liveness = get(port, "/actuator/health/liveness");

      assertThat(liveness.statusCode()).isEqualTo(200);

      assertThat(liveness.body())
          .contains("\"status\":\"UP\"")
          .doesNotContain("\"components\"", "\"details\"");

      HttpResponse<String> readiness = get(port, "/actuator/health/readiness");

      assertThat(readiness.statusCode()).isEqualTo(200);

      assertThat(readiness.body()).contains("\"status\":\"UP\"");

      HttpResponse<String> sensitiveEndpoint = get(port, "/actuator/env");

      assertThat(sensitiveEndpoint.statusCode()).isEqualTo(404);
    } finally {
      stop(process);
    }
  }

  private static Process startJar(Path jar, int port, Path log) throws IOException {
    Path javaExecutable =
        Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java");

    ProcessBuilder processBuilder =
        new ProcessBuilder(javaExecutable.toString(), "-jar", jar.toAbsolutePath().toString());

    processBuilder.redirectErrorStream(true);
    processBuilder.redirectOutput(log.toFile());

    processBuilder.environment().put("SERVER_PORT", Integer.toString(port));

    processBuilder.environment().remove("SPRING_PROFILES_ACTIVE");

    return processBuilder.start();
  }

  private static void waitUntilHealthy(Process process, int port, Path log)
      throws InterruptedException {
    Instant deadline = Instant.now().plus(STARTUP_TIMEOUT);

    while (Instant.now().isBefore(deadline)) {
      if (!process.isAlive()) {
        throw new AssertionError(
            "Notification Service JAR exited before "
                + "becoming healthy."
                + System.lineSeparator()
                + readLog(log));
      }

      try {
        HttpResponse<String> response = get(port, "/actuator/health/liveness");

        if (response.statusCode() == 200 && response.body().contains("\"status\":\"UP\"")) {
          return;
        }
      } catch (IOException ignored) {
        // The server may still be starting.
      }

      Thread.sleep(250);
    }

    throw new AssertionError(
        "Notification Service JAR did not become healthy "
            + "within "
            + STARTUP_TIMEOUT
            + "."
            + System.lineSeparator()
            + readLog(log));
  }

  private static HttpResponse<String> get(int port, String path)
      throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + path))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static Path findExecutableJar() throws IOException {
    Path target = Path.of("target");

    try (var paths = Files.list(target)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(
              path -> {
                String name = path.getFileName().toString();

                return name.startsWith("venueflow-notification-service-")
                    && name.endsWith(".jar")
                    && !name.endsWith("-sources.jar")
                    && !name.endsWith("-javadoc.jar")
                    && !name.endsWith("-tests.jar");
              })
          .sorted(Comparator.comparing(Path::toString))
          .findFirst()
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "Executable Notification " + "Service JAR was " + "not found"));
    }
  }

  private static int reservePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);

      return socket.getLocalPort();
    }
  }

  private static void stop(Process process) throws InterruptedException {
    if (!process.isAlive()) {
      return;
    }

    process.destroy();

    if (!process.waitFor(5, TimeUnit.SECONDS)) {
      process.destroyForcibly();

      process.waitFor(5, TimeUnit.SECONDS);
    }

    assertThat(process.isAlive()).isFalse();
  }

  private static String readLog(Path log) {
    try {
      if (Files.notExists(log)) {
        return "No process log was created.";
      }

      return Files.readString(log);
    } catch (IOException exception) {
      return "Unable to read process log: " + exception.getMessage();
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }
}
