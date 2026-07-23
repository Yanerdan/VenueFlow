package com.yanerdan.venueflow.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class NotificationServiceConfigurationBoundaryTest {

  private static final Path RESOURCES = Path.of("src", "main", "resources");

  private static final Pattern ENVIRONMENT_VARIABLE =
      Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)(?::[^}]*)?}");

  @Test
  void trackedConfigurationContainsNoInfrastructureOrSecrets() throws IOException {
    String configuration = readConfiguration().toLowerCase(Locale.ROOT);

    assertThat(configuration)
        .doesNotContain(
            "jdbc:",
            "rabbitmq",
            "amqp",
            "password",
            "username",
            "spring.mail",
            "redis",
            "kafka",
            "nacos",
            "http://",
            "https://");
  }

  @Test
  void serverPortIsTheOnlyEnvironmentVariable() throws IOException {
    Matcher matcher = ENVIRONMENT_VARIABLE.matcher(readConfiguration());

    Set<String> variables = new TreeSet<>();

    while (matcher.find()) {
      variables.add(matcher.group(1));
    }

    assertThat(variables).containsExactly("SERVER_PORT");
  }

  @Test
  void localSecretFilesAreNotPresent() {
    assertThat(Path.of(".env")).doesNotExist();

    assertThat(Path.of(".env.local")).doesNotExist();

    assertThat(RESOURCES.resolve("application-local.yml")).doesNotExist();

    assertThat(RESOURCES.resolve("application-prod.yml")).doesNotExist();
  }

  private static String readConfiguration() throws IOException {
    return Files.readString(RESOURCES.resolve("application.yml"))
        + System.lineSeparator()
        + Files.readString(RESOURCES.resolve("application-skeleton.yml"));
  }
}
