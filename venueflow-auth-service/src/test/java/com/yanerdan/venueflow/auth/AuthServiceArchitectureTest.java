package com.yanerdan.venueflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

class AuthServiceArchitectureTest {

  @Test
  void mainSourceStaysInsideTheAuthBoundary() throws IOException {
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

    assertThat(sourceFiles).isNotEmpty();
    assertThat(sourceFiles).allMatch(path -> path.startsWith("com/yanerdan/venueflow/auth/"));
  }

  @Test
  void forbiddenLibrariesAreAbsent() {
    ClassLoader classLoader = getClass().getClassLoader();

    List<String> forbiddenClasses =
        List.of(
            "com.baomidou.mybatisplus.core.mapper.BaseMapper",
            "org.springframework.amqp.rabbit.connection.ConnectionFactory",
            "org.springframework.data.redis.core.RedisTemplate",
            "org.springframework.cloud.client.discovery.DiscoveryClient",
            "jakarta.persistence.Entity");

    assertThat(forbiddenClasses)
        .allSatisfy(
            className ->
                assertThat(ClassUtils.isPresent(className, classLoader))
                    .as("%s must be absent", className)
                    .isFalse());
  }
}
