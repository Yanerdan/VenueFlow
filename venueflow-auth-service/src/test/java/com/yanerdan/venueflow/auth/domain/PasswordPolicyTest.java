package com.yanerdan.venueflow.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

  private final PasswordPolicy policy = new PasswordPolicy();

  @Test
  void normalizesValidUsernameAndAcceptsStrongPassword() {
    assertThat(policy.normalizeUsername("  Alice.Example ")).isEqualTo("alice.example");
    policy.validatePassword("VenueFlow2026!".toCharArray());
  }

  @Test
  void rejectsInvalidUsernameAndWeakPassword() {
    assertThatThrownBy(() -> policy.normalizeUsername("a"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> policy.validatePassword("all-lowercase".toCharArray()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
