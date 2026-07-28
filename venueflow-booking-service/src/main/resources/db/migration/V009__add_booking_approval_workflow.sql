ALTER TABLE booking_reservation
  ADD COLUMN approval_mode VARCHAR(24) NOT NULL DEFAULT 'DIRECT'
    AFTER assigned_approver_external_user_id,
  ADD COLUMN final_assigned_approver_external_user_id VARCHAR(64) NULL AFTER approval_mode,
  ADD COLUMN current_approval_step INT NOT NULL DEFAULT 1
    AFTER final_assigned_approver_external_user_id;

CREATE TABLE booking_approval_action (
  id BIGINT NOT NULL AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  approval_step INT NOT NULL,
  actor_external_user_id VARCHAR(64) NULL,
  actor_role VARCHAR(32) NOT NULL,
  decision VARCHAR(24) NOT NULL,
  review_note VARCHAR(1000) NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_booking_approval_action_booking (booking_id, approval_step, id)
);
