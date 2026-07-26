package com.yanerdan.venueflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GovernedRouteConfigurationTest {

  @Test
  void governanceRoutesRemainExplicitAndDiscoveryLocatorStaysDisabled() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/com/yanerdan/venueflow/gateway/config/GatewayRouteConfiguration.java"));
    String configuration =
        Files.readString(Path.of("src/main/resources/application-governance.yml"))
            + Files.readString(Path.of("src/main/resources/application-gateway.yml"));

    assertThat(source)
        .contains(
            "\"/api/v1/auth/**\"",
            "\"/api/v1/users/**\"",
            "\"/api/v1/resources/**\"",
            "\"/api/v1/bookings/**\"",
            "URI.create(\"lb://\" + serviceId)")
        .doesNotContain("DiscoveryClientRouteDefinitionLocator");
    assertThat(configuration).contains("locator:\n          enabled: false");
  }
}
