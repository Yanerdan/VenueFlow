package com.yanerdan.venueflow.resource.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ChangeResourceBookingRulesCommandTest {

  @Test
  void normalizesNotice() {
    ChangeResourceBookingRulesCommand command =
        new ChangeResourceBookingRulesCommand(1L, " Campus card required ", 0, 90, 480, 1L);
    assertThat(command.bookingNotice()).isEqualTo("Campus card required");
  }

  @Test
  void rejectsOutOfRangeRules() {
    assertThatThrownBy(() -> new ChangeResourceBookingRulesCommand(1L, null, -1, 90, 480, 1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("minAdvanceHours");
  }
}
