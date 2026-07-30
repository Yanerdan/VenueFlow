package com.yanerdan.venueflow.resource.catalog.application;

import com.yanerdan.venueflow.resource.catalog.domain.ApprovalMode;
import java.sql.PreparedStatement;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("persistence")
public class ApprovalPolicyService {

  private final JdbcTemplate jdbc;
  private final CatalogApplicationService catalog;

  public ApprovalPolicyService(JdbcTemplate jdbc, CatalogApplicationService catalog) {
    this.jdbc = jdbc;
    this.catalog = catalog;
  }

  @Transactional(readOnly = true)
  public List<ApprovalStageResult> stages(long resourceId) {
    return jdbc.query(
        """
        SELECT s.stage_order, s.stage_name, s.approver_external_user_id
        FROM resource_approval_stage s
        JOIN resource_approval_policy p ON p.id = s.policy_id
        WHERE p.resource_id = ?
        ORDER BY s.stage_order
        """,
        (result, row) ->
            new ApprovalStageResult(
                result.getInt("stage_order"),
                result.getString("stage_name"),
                result.getString("approver_external_user_id")),
        resourceId);
  }

  @Transactional
  public ResourceResult replace(ReplaceApprovalPolicyCommand command) {
    ResourceResult current = catalog.getResource(command.resourceId());
    ApprovalStageResult first = command.stages().getFirst();
    ApprovalStageResult second = command.stages().size() > 1 ? command.stages().get(1) : null;
    ResourceResult updated =
        catalog.changeResourceOwnership(
            new ChangeResourceOwnershipCommand(
                command.resourceId(),
                current.ownerDepartment(),
                first.approverExternalUserId(),
                command.stages().size() > 1 ? ApprovalMode.TWO_STAGE : ApprovalMode.DIRECT,
                second == null ? null : second.approverExternalUserId(),
                command.expectedVersion()));
    jdbc.update(
        """
        INSERT INTO resource_approval_policy(resource_id, policy_name)
        VALUES (?, ?)
        ON DUPLICATE KEY UPDATE policy_name = VALUES(policy_name), updated_at = CURRENT_TIMESTAMP(6)
        """,
        command.resourceId(),
        command.policyName());
    Long policyId =
        jdbc.queryForObject(
            "SELECT id FROM resource_approval_policy WHERE resource_id = ?",
            Long.class,
            command.resourceId());
    jdbc.update("DELETE FROM resource_approval_stage WHERE policy_id = ?", policyId);
    jdbc.batchUpdate(
        """
        INSERT INTO resource_approval_stage
          (policy_id, stage_order, stage_name, approver_external_user_id)
        VALUES (?, ?, ?, ?)
        """,
        command.stages(),
        command.stages().size(),
        (PreparedStatement statement, ApprovalStageResult stage) -> {
          statement.setLong(1, policyId);
          statement.setInt(2, stage.stageOrder());
          statement.setString(3, stage.stageName().trim());
          statement.setString(4, stage.approverExternalUserId().trim());
        });
    return updated.withApprovalStages(command.stages());
  }

  public ResourceResult attach(ResourceResult result) {
    return result.withApprovalStages(stages(result.id()));
  }
}
