package com.yanerdan.venueflow.resource.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResourceStatusTransitionsTest {

  @Test
  void implementsCompleteResourceStatusTransitionMatrix() {
    Map<ResourceStatus, Set<ResourceStatus>> allowedTransitions =
        new EnumMap<>(ResourceStatus.class);

    allowedTransitions.put(
        ResourceStatus.DRAFT, EnumSet.of(ResourceStatus.ACTIVE, ResourceStatus.ARCHIVED));

    allowedTransitions.put(
        ResourceStatus.ACTIVE, EnumSet.of(ResourceStatus.SUSPENDED, ResourceStatus.ARCHIVED));

    allowedTransitions.put(
        ResourceStatus.SUSPENDED, EnumSet.of(ResourceStatus.ACTIVE, ResourceStatus.ARCHIVED));

    allowedTransitions.put(ResourceStatus.ARCHIVED, EnumSet.noneOf(ResourceStatus.class));

    for (ResourceStatus currentStatus : ResourceStatus.values()) {
      for (ResourceStatus targetStatus : ResourceStatus.values()) {
        boolean expected = allowedTransitions.get(currentStatus).contains(targetStatus);

        assertThat(ResourceStatusTransitions.canTransition(currentStatus, targetStatus))
            .as("transition from %s to %s", currentStatus, targetStatus)
            .isEqualTo(expected);
      }
    }
  }

  @Test
  void rejectsNullCurrentStatus() {
    assertThatNullPointerException()
        .isThrownBy(() -> ResourceStatusTransitions.canTransition(null, ResourceStatus.ACTIVE))
        .withMessage("currentStatus must not be null");
  }

  @Test
  void rejectsNullTargetStatus() {
    assertThatNullPointerException()
        .isThrownBy(() -> ResourceStatusTransitions.canTransition(ResourceStatus.DRAFT, null))
        .withMessage("targetStatus must not be null");
  }
}
