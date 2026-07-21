package com.yanerdan.venueflow.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ResourceServiceExecutableJarIT {

  @Test
  @Timeout(90)
  void startsPackagedJarOnOverriddenPortAndServesProbes() throws Exception {
    Path jar = Path.of(requiredSystemProperty("resourceServiceJar")).toAbsolutePath();
    assertThat(jar).isRegularFile();

    int port = findAvailablePort();
    ProcessBuilder processBuilder =
        new ProcessBuilder(javaExecutable().toString(), "-jar", jar.toString());
    processBuilder.environment().put("SERVER_PORT", Integer.toString(port));
    processBuilder.redirectErrorStream(true);

    Process process = processBuilder.start();
    StringBuffer logs = new StringBuffer();
    Thread logReader =
        Thread.startVirtualThread(
            () -> {
              try (BufferedReader reader =
                  new BufferedReader(
                      new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                reader.lines().forEach(line -> logs.append(line).append(System.lineSeparator()));
              } catch (IOException exception) {
                logs.append("Unable to read process output: ").append(exception.getMessage());
              }
            });

    try {
      waitForProbe(process, port, "liveness", logs);
      waitForProbe(process, port, "readiness", logs);
    } finally {
      stopOwnedProcess(process);
      logReader.join(Duration.ofSeconds(5));
    }
  }

  private static void waitForProbe(Process process, int port, String group, StringBuffer logs)
      throws Exception {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    URI uri = URI.create("http://127.0.0.1:" + port + "/actuator/health/" + group);
    Instant deadline = Instant.now().plusSeconds(60);

    while (Instant.now().isBefore(deadline)) {
      if (!process.isAlive()) {
        fail("Resource Service exited before " + group + " was ready.\n" + logs);
      }
      try {
        HttpRequest request =
            HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 && response.body().contains("\"status\":\"UP\"")) {
          return;
        }
      } catch (IOException ignoredWhileStarting) {
        // The HTTP listener is expected to refuse connections during bounded startup.
      }
      Thread.sleep(Duration.ofMillis(250));
    }

    fail("Timed out waiting for " + group + " from packaged Resource Service.\n" + logs);
  }

  private static void stopOwnedProcess(Process process) throws InterruptedException {
    process.destroy();
    if (!process.waitFor(5, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      assertThat(process.waitFor(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  private static int findAvailablePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    }
  }

  private static Path javaExecutable() {
    String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
    Path path = Path.of(System.getProperty("java.home"), "bin", executable);
    assertThat(Files.isExecutable(path)).isTrue();
    return path;
  }

  private static String requiredSystemProperty(String name) {
    String value = System.getProperty(name);
    assertThat(value).as(name).isNotBlank();
    return value;
  }
}
