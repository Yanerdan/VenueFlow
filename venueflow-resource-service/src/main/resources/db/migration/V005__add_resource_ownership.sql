ALTER TABLE `resource`
  ADD COLUMN owner_department VARCHAR(160) NULL AFTER capacity,
  ADD COLUMN approver_external_user_id BIGINT NULL AFTER owner_department;
