package com.yanerdan.venueflow.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "management.server.port=0")
@ActiveProfiles("skeleton")
class SearchServiceApplicationTest {

  @Test
  void startsWithoutExternalConnections() {
    assertThat(true).isTrue();
  }
}
