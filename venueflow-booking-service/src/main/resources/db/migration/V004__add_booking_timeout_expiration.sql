ALTER TABLE booking_reservation
  DROP CHECK ck_booking_reservation_status,
  MODIFY confirmed_at DATETIME(6) NULL,
  ADD COLUMN expire_at DATETIME(6) NULL AFTER created_at,
  ADD COLUMN expired_at DATETIME(6) NULL AFTER cancelled_at,
  ADD COLUMN terminal_reason VARCHAR(64) NULL AFTER expired_at,
  ADD COLUMN timeout_state VARCHAR(16) NULL AFTER terminal_reason,
  ADD COLUMN timeout_lease_owner VARCHAR(64) NULL AFTER timeout_state,
  ADD COLUMN timeout_lease_expires_at DATETIME(6) NULL AFTER timeout_lease_owner,
  ADD COLUMN timeout_attempt_count INT NOT NULL DEFAULT 0 AFTER timeout_lease_expires_at,
  ADD COLUMN timeout_next_check_at DATETIME(6) NULL AFTER timeout_attempt_count,
  ADD COLUMN timeout_last_error_code VARCHAR(64) NULL AFTER timeout_next_check_at;

UPDATE booking_reservation SET timeout_state = 'COMPLETED';

ALTER TABLE booking_reservation
  MODIFY timeout_state VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
  ADD KEY idx_booking_timeout_due
    (status, timeout_state, timeout_next_check_at, timeout_lease_expires_at, id),
  ADD CONSTRAINT ck_booking_reservation_status CHECK (
    status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED', 'EXPIRED')
  ),
  ADD CONSTRAINT ck_booking_timeout_state CHECK (
    timeout_state IN ('IDLE', 'LEASED', 'RETRY', 'EXHAUSTED', 'COMPLETED')
  ),
  ADD CONSTRAINT ck_booking_timeout_attempt CHECK (timeout_attempt_count >= 0);

CREATE TABLE booking_status_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  from_status VARCHAR(24) NULL,
  to_status VARCHAR(24) NOT NULL,
  source VARCHAR(24) NOT NULL,
  reason_code VARCHAR(64) NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_booking_status_transition (booking_id, to_status),
  KEY idx_booking_status_log_booking (booking_id, created_at),
  CONSTRAINT fk_booking_status_log_booking
    FOREIGN KEY (booking_id) REFERENCES booking_reservation(id)
);

ALTER TABLE booking_outbox_event
  DROP CHECK ck_booking_outbox_event_type,
  ADD CONSTRAINT ck_booking_outbox_event_type CHECK (
    event_type IN (
      'BOOKING_RESERVATION_CONFIRMED',
      'BOOKING_RESERVATION_CANCELLED',
      'BOOKING_RESERVATION_EXPIRED'
    )
  );
