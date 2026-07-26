package com.yanerdan.venueflow.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MicroserviceGovernanceConfigurationTest {

  private static final List<String> MODULES =
      List.of(
          "venueflow-gateway",
          "venueflow-auth-service",
          "venueflow-user-service",
          "venueflow-resource-service",
          "venueflow-booking-service",
          "venueflow-notification-service");

  @Test
  void everyExecutableModuleHasBoundedGovernanceConfiguration() throws IOException {
    Path repository = Path.of("..").toAbsolutePath().normalize();
    for (String module : MODULES) {
      String application =
          Files.readString(
              repository.resolve(module).resolve("src/main/resources/application.yml"));
      String governance =
          Files.readString(
              repository.resolve(module).resolve("src/main/resources/application-governance.yml"));

      assertThat(application)
          .as(module)
          .contains("discovery:\n      enabled: false", "config:\n        enabled: false");
      assertThat(governance)
          .as(module)
          .contains(
              "on-profile: governance",
              "optional:nacos:venueflow-common.yml",
              "${NACOS_SERVER_ADDR}",
              "${NACOS_NAMESPACE}",
              "VENUEFLOW_GROUP",
              "auto-registration:\n        enabled: true")
          .doesNotContain("BEGIN PRIVATE KEY", "replace-with-local-secret");
    }
  }
}
