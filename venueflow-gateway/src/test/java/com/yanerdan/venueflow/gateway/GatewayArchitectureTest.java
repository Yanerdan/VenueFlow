package com.yanerdan.venueflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

class GatewayArchitectureTest {

  @Test
  void mainSourceStaysInsideGatewayBoundary() throws IOException {
    Path sourceRoot = Path.of("src", "main", "java");
    try (var paths = Files.walk(sourceRoot)) {
      assertThat(
              paths
                  .filter(Files::isRegularFile)
                  .filter(path -> path.toString().endsWith(".java"))
                  .map(sourceRoot::relativize)
                  .map(Path::toString)
                  .map(path -> path.replace('\\', '/'))
                  .toList())
          .isNotEmpty()
          .allMatch(path -> path.startsWith("com/yanerdan/venueflow/gateway/"));
    }
  }

  @Test
  void forbiddenLibrariesAreAbsent() {
    ClassLoader classLoader = getClass().getClassLoader();
    List.of(
            "jakarta.servlet.Servlet",
            "jakarta.persistence.Entity",
            "org.springframework.data.redis.core.RedisTemplate",
            "org.springframework.amqp.rabbit.connection.ConnectionFactory")
        .forEach(
            className ->
                assertThat(ClassUtils.isPresent(className, classLoader))
                    .as("%s must be absent", className)
                    .isFalse());
  }
}
