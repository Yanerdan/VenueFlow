package com.yanerdan.venueflow.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class StabilityObservabilityConfigurationTest {

  private static final Path REPOSITORY = Path.of("..").toAbsolutePath().normalize();
  private static final List<String> EXECUTABLE_MODULES =
      List.of(
          "venueflow-gateway",
          "venueflow-auth-service",
          "venueflow-user-service",
          "venueflow-resource-service",
          "venueflow-booking-service",
          "venueflow-notification-service",
          "venueflow-search-service");

  @Test
  void keepsTelemetryOptInAndSentinelScopeLimited() throws IOException {
    for (String module : EXECUTABLE_MODULES) {
      Path resources = REPOSITORY.resolve(module).resolve("src/main/resources");
      assertThat(Files.readString(resources.resolve("application.yml")))
          .contains("opentelemetry:", "enabled: false")
          .contains("include: health");
      assertThat(Files.readString(resources.resolve("application-observe.yml")))
          .contains("on-profile: observe")
          .contains("include: health,prometheus")
          .contains("${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT:");
    }

    for (String module :
        List.of("venueflow-gateway", "venueflow-booking-service", "venueflow-search-service")) {
      assertThat(Files.readString(REPOSITORY.resolve(module).resolve("pom.xml")))
          .contains("spring-cloud-starter-alibaba-sentinel");
      assertThat(
              Files.readString(
                  REPOSITORY
                      .resolve(module)
                      .resolve("src/main/resources/application-stability.yml")))
          .contains("on-profile: stability", "enabled: true", "eager: false");
    }

    for (String module :
        List.of(
            "venueflow-auth-service",
            "venueflow-user-service",
            "venueflow-resource-service",
            "venueflow-notification-service")) {
      assertThat(Files.readString(REPOSITORY.resolve(module).resolve("pom.xml")))
          .doesNotContain("<artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>");
    }
  }

  @Test
  void keepsTimeoutsStrictlyNested() throws IOException {
    String gateway =
        Files.readString(
            REPOSITORY.resolve("venueflow-gateway/src/main/resources/application-stability.yml"));
    String booking =
        Files.readString(
            REPOSITORY.resolve(
                "venueflow-booking-service/src/main/resources/application-stability.yml"));
    String governance =
        Files.readString(
            REPOSITORY.resolve(
                "venueflow-booking-service/src/main/resources/application-governance.yml"));

    int gatewayBudget = defaultNumber(gateway, "VENUEFLOW_GATEWAY_RESPONSE_TIMEOUT_MS");
    int bookingBudget = defaultNumber(booking, "VENUEFLOW_BOOKING_REQUEST_TIMEOUT_MS");
    int readBudget = defaultNumber(governance, "VENUEFLOW_COLLABORATOR_REQUEST_TIMEOUT_MS");
    int connectBudget = defaultNumber(governance, "VENUEFLOW_COLLABORATOR_CONNECT_TIMEOUT_MS");

    assertThat(gatewayBudget).isGreaterThan(bookingBudget);
    assertThat(bookingBudget).isGreaterThan(readBudget);
    assertThat(readBudget).isGreaterThan(connectBudget);
  }

  @Test
  void provisionsDisabledRulesAndBoundedObserveStack() throws IOException {
    for (String rule : List.of("gateway-rules.json", "booking-rules.json", "search-rules.json")) {
      assertThat(Files.readString(REPOSITORY.resolve("deploy/sentinel").resolve(rule)))
          .contains("\"enabled\": false")
          .contains("\"threshold\": null")
          .contains("\"evidenceRequired\": true");
    }

    String compose = Files.readString(REPOSITORY.resolve("deploy/compose/compose.yml"));
    assertThat(compose)
        .contains("profiles: [observe]")
        .contains("prometheus:", "grafana:", "otel-collector:", "jaeger:")
        .contains("mem_limit:", "healthcheck:");

    for (String dashboard : List.of("system.json", "gateway.json", "booking.json", "search.json")) {
      assertThat(
              Files.readString(
                  REPOSITORY.resolve("deploy/observability/grafana/dashboards").resolve(dashboard)))
          .contains("\"uid\":", "\"panels\":");
    }
  }

  @Test
  void preservesBoundedMetricsAndAsyncTraceIdentity() throws IOException {
    String notificationListener =
        Files.readString(
            REPOSITORY.resolve(
                "venueflow-notification-service/src/main/java/com/yanerdan/venueflow/"
                    + "notification/consumer/messaging/NotificationMessageListener.java"));
    String resourceRecorder =
        Files.readString(
            REPOSITORY.resolve(
                "venueflow-resource-service/src/main/java/com/yanerdan/venueflow/"
                    + "resource/event/PersistentResourceChangeRecorder.java"));

    assertThat(notificationListener)
        .contains("venueflow.notification.received", "venueflow.notification.ack")
        .doesNotContain(".tag(\"user", ".tag(\"query", ".tag(\"token");
    assertThat(resourceRecorder).contains("envelope.put(\"traceId\"");
  }

  private static int defaultNumber(String configuration, String environmentName) {
    String marker = "${" + environmentName + ":";
    int start = configuration.indexOf(marker);
    assertThat(start).isGreaterThanOrEqualTo(0);
    int valueStart = start + marker.length();
    return Integer.parseInt(
        configuration.substring(valueStart, configuration.indexOf('}', valueStart)));
  }
}
