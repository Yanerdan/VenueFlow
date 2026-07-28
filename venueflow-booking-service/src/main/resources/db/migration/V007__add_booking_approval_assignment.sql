ALTER TABLE booking_reservation
  ADD COLUMN resource_id BIGINT NULL AFTER slot_id,
  ADD COLUMN owner_department VARCHAR(160) NULL AFTER resource_id,
  ADD COLUMN assigned_approver_external_user_id BIGINT NULL AFTER owner_department,
  ADD INDEX idx_booking_assigned_approver_status
    (assigned_approver_external_user_id, status, created_at);
