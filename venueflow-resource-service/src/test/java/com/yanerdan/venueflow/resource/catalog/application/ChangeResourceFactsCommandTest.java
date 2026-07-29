package com.yanerdan.venueflow.resource.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ChangeResourceFactsCommandTest {

  @Test
  void normalizesValidatedFacts() {
    ChangeResourceFactsCommand command =
        new ChangeResourceFactsCommand(1L, 2L, " Room ", " Notes ", " Building A ", 30, 4L);

    assertThat(command.name()).isEqualTo("Room");
    assertThat(command.description()).isEqualTo("Notes");
    assertThat(command.location()).isEqualTo("Building A");
  }

  @Test
  void rejectsBlankPublicLocation() {
    assertThatThrownBy(() -> new ChangeResourceFactsCommand(1L, 2L, "Room", null, " ", 30, 4L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("location");
  }
}
