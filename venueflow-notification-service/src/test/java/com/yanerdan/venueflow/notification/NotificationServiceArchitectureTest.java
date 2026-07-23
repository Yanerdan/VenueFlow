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
  void mainSourceContainsOnlyTheSkeletonEntryPoint() throws IOException {
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
        .containsExactly(
            "com/yanerdan/venueflow/notification/" + "NotificationServiceApplication.java");
  }

  @Test
  void forbiddenInfrastructureLibrariesAreAbsent() {
    ClassLoader classLoader = getClass().getClassLoader();

    List<String> forbiddenClasses =
        List.of(
            "org.springframework.jdbc.core.JdbcTemplate",
            "org.flywaydb.core.Flyway",
            "com.baomidou.mybatisplus.core.mapper.BaseMapper",
            "org.springframework.amqp.rabbit.connection.ConnectionFactory",
            "com.rabbitmq.client.ConnectionFactory",
            "org.springframework.mail.javamail.JavaMailSender",
            "org.springframework.security.core.Authentication",
            "org.springframework.data.redis.core.RedisTemplate",
            "org.springframework.kafka.core.KafkaTemplate",
            "org.springframework.cloud.client.discovery.DiscoveryClient",
            "org.testcontainers.containers.GenericContainer");

    assertThat(forbiddenClasses)
        .allSatisfy(
            className ->
                assertThat(ClassUtils.isPresent(className, classLoader))
                    .as("%s must be absent", className)
                    .isFalse());
  }
}
