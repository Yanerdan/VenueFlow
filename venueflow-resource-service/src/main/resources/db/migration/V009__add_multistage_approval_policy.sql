CREATE TABLE resource_approval_policy (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resource_id BIGINT NOT NULL,
  policy_name VARCHAR(100) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_resource_approval_policy_resource (resource_id),
  CONSTRAINT fk_resource_approval_policy_resource FOREIGN KEY (resource_id) REFERENCES resource (id)
);

CREATE TABLE resource_approval_stage (
  id BIGINT NOT NULL AUTO_INCREMENT,
  policy_id BIGINT NOT NULL,
  stage_order INT NOT NULL,
  stage_name VARCHAR(100) NOT NULL,
  approver_external_user_id VARCHAR(64) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_resource_approval_stage_order (policy_id, stage_order),
  CONSTRAINT fk_resource_approval_stage_policy
    FOREIGN KEY (policy_id) REFERENCES resource_approval_policy (id) ON DELETE CASCADE
);

INSERT INTO resource_approval_policy(resource_id, policy_name)
SELECT id, '默认审批流程' FROM resource WHERE approver_external_user_id IS NOT NULL;

INSERT INTO resource_approval_stage(policy_id, stage_order, stage_name, approver_external_user_id)
SELECT p.id, 1, '资源负责人审批', r.approver_external_user_id
FROM resource_approval_policy p JOIN resource r ON r.id = p.resource_id;

INSERT INTO resource_approval_stage(policy_id, stage_order, stage_name, approver_external_user_id)
SELECT p.id, 2, '最终审批', r.final_approver_external_user_id
FROM resource_approval_policy p JOIN resource r ON r.id = p.resource_id
WHERE r.approval_mode = 'TWO_STAGE' AND r.final_approver_external_user_id IS NOT NULL;
