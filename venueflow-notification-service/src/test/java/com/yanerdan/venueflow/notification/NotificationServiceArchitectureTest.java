package com.yanerdan.venueflow.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

class NotificationServiceArchitectureTest {

  @Test
  void mainSourceStaysInsideNotificationBoundary() throws IOException {
    Path sourceRoot = Path.of("src", "main", "java");

    List<String> sourceFiles;

    try (var paths = Files.walk(sourceRoot)) {
      sourceFiles =
          paths
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".java"))
              .map(sourceRoot::relativize)
              .map(Path::toString)
              .map(path -> path.replace('\\', '/'))
              .sorted()
              .toList();
    }

    assertThat(sourceFiles)
        .isNotEmpty()
        .allSatisfy(
            file ->
                assertThat(file)
                    .startsWith("com/yanerdan/venueflow/notification/")
                    .doesNotContain("/web/", "/mail/", "/security/", "/client/"));
  }

  @Test
  void forbiddenInfrastructureLibrariesAreAbsent() {
    ClassLoader classLoader = getClass().getClassLoader();

    List<String> forbiddenClasses =
        List.of(
            "com.baomidou.mybatisplus.core.mapper.BaseMapper",
            "org.springframework.mail.javamail.JavaMailSender",
            "org.springframework.security.core.Authentication",
            "org.springframework.data.redis.core.RedisTemplate",
            "org.springframework.kafka.core.KafkaTemplate",
            "org.springframework.cloud.client.discovery.DiscoveryClient");

    assertThat(forbiddenClasses)
        .allSatisfy(
            className ->
                assertThat(ClassUtils.isPresent(className, classLoader))
                    .as("%s must be absent", className)
                    .isFalse());
  }
}
