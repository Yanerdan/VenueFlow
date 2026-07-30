CREATE TABLE booking_approval_stage_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  stage_order INT NOT NULL,
  stage_name VARCHAR(100) NOT NULL,
  approver_external_user_id VARCHAR(64) NOT NULL,
  stage_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  decided_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_booking_approval_stage_order (booking_id, stage_order),
  KEY idx_booking_approval_stage_assignee (approver_external_user_id, stage_status),
  CONSTRAINT fk_booking_approval_stage_booking
    FOREIGN KEY (booking_id) REFERENCES booking_reservation (id)
);

INSERT INTO booking_approval_stage_snapshot
  (booking_id, stage_order, stage_name, approver_external_user_id, stage_status, decided_at)
SELECT id, 1, '资源负责人审批', assigned_approver_external_user_id,
       CASE WHEN current_approval_step > 1 OR status = 'CONFIRMED' THEN 'APPROVED' ELSE 'PENDING' END,
       CASE WHEN current_approval_step > 1 OR status = 'CONFIRMED' THEN reviewed_at ELSE NULL END
FROM booking_reservation
WHERE assigned_approver_external_user_id IS NOT NULL;

INSERT INTO booking_approval_stage_snapshot
  (booking_id, stage_order, stage_name, approver_external_user_id, stage_status, decided_at)
SELECT id, 2, '最终审批', final_assigned_approver_external_user_id,
       CASE WHEN status = 'CONFIRMED' THEN 'APPROVED' ELSE 'PENDING' END,
       CASE WHEN status = 'CONFIRMED' THEN reviewed_at ELSE NULL END
FROM booking_reservation
WHERE approval_mode = 'TWO_STAGE' AND final_assigned_approver_external_user_id IS NOT NULL;
