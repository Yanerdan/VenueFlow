CREATE TABLE booking_outbox_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(36) NOT NULL,
  aggregate_type VARCHAR(32) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  event_version INT NOT NULL,
  routing_key VARCHAR(96) NOT NULL,
  payload VARCHAR(4096) NOT NULL,
  headers VARCHAR(1024) NOT NULL,
  status VARCHAR(16) NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME(6) NULL,
  claim_token VARCHAR(36) NULL,
  lease_until DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  published_at DATETIME(6) NULL,
  last_error_code VARCHAR(64) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_booking_outbox_event_id (event_id),
  UNIQUE KEY uk_booking_outbox_business_event
    (aggregate_type, aggregate_id, event_type, event_version),
  KEY idx_booking_outbox_scan (status, next_retry_at, lease_until, id),
  CONSTRAINT ck_booking_outbox_aggregate CHECK (aggregate_type = 'BOOKING'),
  CONSTRAINT ck_booking_outbox_event_type CHECK (
    event_type IN ('BOOKING_RESERVATION_CONFIRMED', 'BOOKING_RESERVATION_CANCELLED')
  ),
  CONSTRAINT ck_booking_outbox_status CHECK (
    status IN ('NEW', 'PUBLISHING', 'RETRY', 'PUBLISHED', 'DEAD')
  ),
  CONSTRAINT ck_booking_outbox_version CHECK (event_version > 0),
  CONSTRAINT ck_booking_outbox_retry CHECK (retry_count >= 0)
);
