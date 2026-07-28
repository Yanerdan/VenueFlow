ALTER TABLE resource
  ADD COLUMN approval_mode VARCHAR(24) NOT NULL DEFAULT 'DIRECT' AFTER approver_external_user_id,
  ADD COLUMN final_approver_external_user_id VARCHAR(64) NULL AFTER approval_mode;
