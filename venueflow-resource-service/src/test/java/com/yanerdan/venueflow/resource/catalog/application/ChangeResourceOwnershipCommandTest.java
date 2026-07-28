package com.yanerdan.venueflow.resource.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yanerdan.venueflow.resource.catalog.domain.ApprovalMode;
import org.junit.jupiter.api.Test;

class ChangeResourceOwnershipCommandTest {
  @Test
  void acceptsDistinctTwoStageApprovers() {
    var command =
        new ChangeResourceOwnershipCommand(
            1L, "校团委", "approver-1", ApprovalMode.TWO_STAGE, "approver-2", 3L);

    assertThat(command.approvalMode()).isEqualTo(ApprovalMode.TWO_STAGE);
    assertThat(command.finalApproverExternalUserId()).isEqualTo("approver-2");
  }

  @Test
  void rejectsRepeatedTwoStageApprover() {
    assertThatThrownBy(
            () ->
                new ChangeResourceOwnershipCommand(
                    1L, "校团委", "same", ApprovalMode.TWO_STAGE, "same", 3L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("distinct");
  }
}
