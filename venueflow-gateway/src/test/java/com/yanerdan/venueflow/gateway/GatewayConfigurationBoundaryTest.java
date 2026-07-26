package com.yanerdan.venueflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GatewayConfigurationBoundaryTest {

  private static final Path RESOURCES = Path.of("src", "main", "resources");

  @Test
  void trackedConfigurationContainsNoUsableSecretOrPrivateKey() throws IOException {
    String configuration =
        Files.readString(RESOURCES.resolve("application.yml"))
            + Files.readString(RESOURCES.resolve("application-skeleton.yml"))
            + Files.readString(RESOURCES.resolve("application-gateway.yml"));

    assertThat(configuration)
        .doesNotContain("BEGIN PRIVATE KEY", "BEGIN RSA PRIVATE KEY", "replace-with-real")
        .contains("jwt-public-key: ${JWT_PUBLIC_KEY}");
  }

  @Test
  void localSecretFilesAreAbsent() {
    assertThat(Path.of(".env")).doesNotExist();
    assertThat(Path.of(".env.local")).doesNotExist();
  }
}
