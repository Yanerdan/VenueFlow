package com.yanerdan.venueflow.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BookingServiceConfigurationBoundaryTest {

  private static final Path MODULE_ROOT =
      Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();

  private static final Path MAIN_RESOURCES = MODULE_ROOT.resolve("src/main/resources");

  private static final List<String> FORBIDDEN_CONFIGURATION_FRAGMENTS =
      List.of(
          "datasource:",
          "jdbc:",
          "username:",
          "password:",
          "secret:",
          "token:",
          "import:",
          "redis",
          "rabbitmq",
          "kafka",
          "feign",
          "http://",
          "https://");

  @Test
  void defaultConfigurationContainsNoExternalInfrastructureOrSecrets() throws IOException {
    List<Path> applicationFiles = applicationConfigurationFiles();

    assertThat(applicationFiles)
        .extracting(path -> MAIN_RESOURCES.relativize(path).toString().replace('\\', '/'))
        .containsExactlyInAnyOrder(
            "application.yml",
            "application-skeleton.yml",
            "application-reconciliation.yml",
            "application-expiration.yml");

    String configuration = readConfiguration(applicationFiles).toLowerCase(Locale.ROOT);

    assertThat(configuration)
        .doesNotContain(FORBIDDEN_CONFIGURATION_FRAGMENTS.toArray(String[]::new))
        .contains("discovery:\n      enabled: false", "config:\n        enabled: false");
  }

  @Test
  void moduleContainsNoInfrastructureOrSecretFiles() throws IOException {
    assertThat(MODULE_ROOT.resolve("compose.yaml")).doesNotExist();
    assertThat(MODULE_ROOT.resolve("compose.yml")).doesNotExist();
    assertThat(MODULE_ROOT.resolve("Dockerfile")).doesNotExist();
    assertThat(environmentFiles()).isEmpty();
    assertThat(sensitiveSourceFiles()).isEmpty();
  }

  private static List<Path> applicationConfigurationFiles() throws IOException {
    try (Stream<Path> paths = Files.list(MAIN_RESOURCES)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().startsWith("application"))
          .filter(path -> !path.getFileName().toString().contains("persistence"))
          .filter(path -> !path.getFileName().toString().contains("messaging"))
          .filter(path -> !path.getFileName().toString().contains("governance"))
          .filter(path -> !path.getFileName().toString().contains("observe"))
          .filter(path -> !path.getFileName().toString().contains("stability"))
          .toList();
    }
  }

  @Test
  void messagingConfigurationUsesEnvironmentCredentialsAndBoundedDefaults() throws IOException {
    String configuration =
        Files.readString(
            MAIN_RESOURCES.resolve("application-messaging.yml"), StandardCharsets.UTF_8);

    assertThat(configuration)
        .contains("on-profile: messaging")
        .contains("${VENUEFLOW_RABBITMQ_HOST}")
        .contains("${VENUEFLOW_RABBITMQ_USERNAME}")
        .contains("${VENUEFLOW_RABBITMQ_PASSWORD}")
        .contains("publisher-confirm-type: correlated")
        .contains("publisher-returns: true")
        .contains("mandatory: true")
        .doesNotContain("guest");
  }

  private static String readConfiguration(List<Path> applicationFiles) throws IOException {
    StringBuilder configuration = new StringBuilder();

    for (Path applicationFile : applicationFiles) {
      configuration.append(Files.readString(applicationFile, StandardCharsets.UTF_8)).append('\n');
    }

    return configuration.toString();
  }

  private static List<String> environmentFiles() throws IOException {
    try (Stream<Path> paths = Files.list(MODULE_ROOT)) {
      return paths
          .filter(Files::isRegularFile)
          .map(path -> path.getFileName().toString())
          .filter(name -> name.equals(".env") || name.startsWith(".env."))
          .toList();
    }
  }

  private static List<String> sensitiveSourceFiles() throws IOException {
    List<String> sensitiveFiles = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(MODULE_ROOT.resolve("src"))) {
      paths
          .filter(Files::isRegularFile)
          .filter(BookingServiceConfigurationBoundaryTest::hasSensitiveExtension)
          .map(path -> MODULE_ROOT.relativize(path).toString().replace('\\', '/'))
          .forEach(sensitiveFiles::add);
    }

    return List.copyOf(sensitiveFiles);
  }

  private static boolean hasSensitiveExtension(Path path) {
    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);

    return name.endsWith(".pem")
        || name.endsWith(".key")
        || name.endsWith(".p12")
        || name.endsWith(".pfx")
        || name.endsWith(".jks")
        || name.endsWith(".keystore");
  }
}
