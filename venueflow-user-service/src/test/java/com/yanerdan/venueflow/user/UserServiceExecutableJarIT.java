package com.yanerdan.venueflow.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.json.BasicJsonTester;

class UserServiceExecutableJarIT {

  private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30);

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

  private static final Duration RETRY_DELAY = Duration.ofMillis(200);

  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

  private final BasicJsonTester json = new BasicJsonTester(getClass());

  @Test
  void packagedJarStartsAndServesOnlyApprovedHealthEndpoints() throws Exception {
    Path targetDirectory = targetDirectory();
    Path executableJar = executableJar(targetDirectory);
    int port = availablePort();

    Path logFile = targetDirectory.resolve("user-service-executable-jar-it.log");

    Files.deleteIfExists(logFile);

    Process process = startExecutableJar(executableJar, port, logFile);

    try {
      HttpResponse<String> liveness =
          awaitSuccessfulResponse(process, port, "/actuator/health/liveness", logFile);

      assertUpResponse(liveness, "/actuator/health/liveness");

      HttpResponse<String> readiness =
          awaitSuccessfulResponse(process, port, "/actuator/health/readiness", logFile);

      assertUpResponse(readiness, "/actuator/health/readiness");

      assertThat(get(port, "/actuator").statusCode()).isEqualTo(404);

      assertThat(get(port, "/actuator/env").statusCode()).isEqualTo(404);
    } finally {
      stop(process);
    }
  }

  @Test
  void serverPortEnvironmentVariableOverridesDefaultPort() throws Exception {
    Path targetDirectory = targetDirectory();

    Path executableJar = executableJar(targetDirectory);

    int overriddenPort = availablePort();

    Path logFile = targetDirectory.resolve("user-service-port-override-it.log");

    Files.deleteIfExists(logFile);

    Process process = startExecutableJarWithEnvironmentPort(executableJar, overriddenPort, logFile);

    try {
      HttpResponse<String> readiness =
          awaitSuccessfulResponse(process, overriddenPort, "/actuator/health/readiness", logFile);

      assertUpResponse(readiness, "/actuator/health/readiness");
    } finally {
      stop(process);
    }
  }

  private static Process startExecutableJarWithEnvironmentPort(
      Path executableJar, int port, Path logFile) throws IOException {
    ProcessBuilder processBuilder =
        new ProcessBuilder(javaExecutable().toString(), "-jar", executableJar.toString());

    processBuilder.environment().put("SERVER_PORT", Integer.toString(port));

    processBuilder.environment().remove("SPRING_PROFILES_ACTIVE");

    processBuilder.redirectErrorStream(true);

    processBuilder.redirectOutput(logFile.toFile());

    return processBuilder.start();
  }

  private void assertUpResponse(HttpResponse<String> response, String path) {
    assertThat(response.statusCode()).as("HTTP status for %s", path).isEqualTo(200);

    assertThat(json.from(response.body()))
        .isStrictlyEqualToJson(
            """
                {
                  "status": "UP"
                }
                """);
  }

  private static HttpResponse<String> awaitSuccessfulResponse(
      Process process, int port, String path, Path logFile) throws Exception {
    long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();

    int lastStatus = -1;
    IOException lastFailure = null;

    while (System.nanoTime() < deadline) {
      if (!process.isAlive()) {
        throw new AssertionError(
            "User Service process exited before "
                + path
                + " became available."
                + System.lineSeparator()
                + readLog(logFile));
      }

      try {
        HttpResponse<String> response = get(port, path);

        lastStatus = response.statusCode();

        if (lastStatus == 200) {
          return response;
        }
      } catch (IOException exception) {
        lastFailure = exception;
      }

      Thread.sleep(RETRY_DELAY.toMillis());
    }

    String failureMessage =
        "Timed out waiting for "
            + path
            + ". Last HTTP status: "
            + lastStatus
            + ". Last connection failure: "
            + lastFailure
            + System.lineSeparator()
            + readLog(logFile);

    throw new AssertionError(failureMessage);
  }

  private static HttpResponse<String> get(int port, String path)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + path))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();

    return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  private static Process startExecutableJar(Path executableJar, int port, Path logFile)
      throws IOException {
    ProcessBuilder processBuilder =
        new ProcessBuilder(
            javaExecutable().toString(), "-jar", executableJar.toString(), "--server.port=" + port);

    processBuilder.environment().remove("SERVER_PORT");

    processBuilder.environment().remove("SPRING_PROFILES_ACTIVE");

    processBuilder.redirectErrorStream(true);

    processBuilder.redirectOutput(logFile.toFile());

    return processBuilder.start();
  }

  private static Path targetDirectory() throws Exception {
    Path testClassesDirectory =
        Path.of(
            UserServiceExecutableJarIT.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());

    return testClassesDirectory.getParent();
  }

  private static Path executableJar(Path targetDirectory) throws IOException {
    List<Path> jars;

    try (Stream<Path> paths = Files.list(targetDirectory)) {
      jars =
          paths
              .filter(Files::isRegularFile)
              .filter(UserServiceExecutableJarIT::isExecutableJarCandidate)
              .toList();
    }

    assertThat(jars).as("packaged User Service executable jars in %s", targetDirectory).hasSize(1);

    return jars.getFirst();
  }

  private static boolean isExecutableJarCandidate(Path path) {
    String fileName = path.getFileName().toString();

    return fileName.startsWith("venueflow-user-service-")
        && fileName.endsWith(".jar")
        && !fileName.endsWith("-sources.jar")
        && !fileName.endsWith("-javadoc.jar")
        && !fileName.endsWith("-tests.jar");
  }

  private static int availablePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
      return socket.getLocalPort();
    }
  }

  private static Path javaExecutable() {
    String executableName =
        System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
            ? "java.exe"
            : "java";

    Path executable = Path.of(System.getProperty("java.home"), "bin", executableName);

    assertThat(executable).exists().isRegularFile();

    return executable;
  }

  private static void stop(Process process) throws InterruptedException {
    if (!process.isAlive()) {
      return;
    }

    process.destroy();

    if (!process.waitFor(5, TimeUnit.SECONDS)) {
      process.destroyForcibly();

      assertThat(process.waitFor(5, TimeUnit.SECONDS)).as("User Service process stopped").isTrue();
    }
  }

  private static String readLog(Path logFile) {
    try {
      if (!Files.exists(logFile)) {
        return "No process log was created.";
      }

      return Files.readString(logFile, StandardCharsets.UTF_8);
    } catch (IOException exception) {
      return "Unable to read process log: " + exception;
    }
  }
}
