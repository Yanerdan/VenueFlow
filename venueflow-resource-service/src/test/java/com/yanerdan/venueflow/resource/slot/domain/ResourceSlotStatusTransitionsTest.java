package com.yanerdan.venueflow.resource.slot.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResourceSlotStatusTransitionsTest {

  @Test
  void permitsOnlyOpenClosedStateChanges() {
    assertThat(
            ResourceSlotStatusTransitions.canTransition(
                ResourceSlotStatus.OPEN, ResourceSlotStatus.CLOSED))
        .isTrue();
    assertThat(
            ResourceSlotStatusTransitions.canTransition(
                ResourceSlotStatus.CLOSED, ResourceSlotStatus.OPEN))
        .isTrue();
    assertThat(
            ResourceSlotStatusTransitions.canTransition(
                ResourceSlotStatus.OPEN, ResourceSlotStatus.OPEN))
        .isFalse();
  }
}
