package com.yanerdan.venueflow.resource.catalog.application;

import java.util.List;

public record ReplaceApprovalPolicyCommand(
    long resourceId, long expectedVersion, String policyName, List<ApprovalStageResult> stages) {

  public ReplaceApprovalPolicyCommand {
    policyName = policyName == null ? "" : policyName.trim();
    stages = stages == null ? List.of() : List.copyOf(stages);
    if (resourceId <= 0
        || expectedVersion <= 0
        || policyName.isBlank()
        || policyName.length() > 100) {
      throw new IllegalArgumentException("Invalid approval policy");
    }
    if (stages.isEmpty() || stages.size() > 5) {
      throw new IllegalArgumentException("Approval policy must contain 1 to 5 stages");
    }
    for (int index = 0; index < stages.size(); index++) {
      ApprovalStageResult stage = stages.get(index);
      if (stage == null
          || stage.stageOrder() != index + 1
          || stage.stageName() == null
          || stage.stageName().isBlank()
          || stage.stageName().length() > 100
          || stage.approverExternalUserId() == null
          || stage.approverExternalUserId().isBlank()
          || stage.approverExternalUserId().length() > 64) {
        throw new IllegalArgumentException("Approval stages must be ordered and complete");
      }
    }
  }
}
