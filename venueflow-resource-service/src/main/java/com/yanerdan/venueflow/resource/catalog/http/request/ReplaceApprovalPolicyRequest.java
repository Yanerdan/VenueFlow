package com.yanerdan.venueflow.resource.catalog.http.request;

import com.yanerdan.venueflow.resource.catalog.application.ApprovalStageResult;
import com.yanerdan.venueflow.resource.catalog.application.ReplaceApprovalPolicyCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReplaceApprovalPolicyRequest(
    @Positive long expectedVersion,
    @NotBlank @Size(max = 100) String policyName,
    @NotEmpty @Size(max = 5) List<@Valid StageRequest> stages) {

  public ReplaceApprovalPolicyRequest {
    stages = List.copyOf(stages);
  }

  public record StageRequest(
      @Positive int stageOrder,
      @NotBlank @Size(max = 100) String stageName,
      @NotBlank @Size(max = 64) String approverExternalUserId) {}

  public ReplaceApprovalPolicyCommand toCommand(long resourceId) {
    return new ReplaceApprovalPolicyCommand(
        resourceId,
        expectedVersion,
        policyName,
        stages.stream()
            .map(
                stage ->
                    new ApprovalStageResult(
                        stage.stageOrder(), stage.stageName(), stage.approverExternalUserId()))
            .toList());
  }
}
