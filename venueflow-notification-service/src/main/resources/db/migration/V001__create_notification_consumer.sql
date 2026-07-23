CREATE TABLE notification_consumed_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  consumer_name VARCHAR(64) NOT NULL,
  event_id VARCHAR(36) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  event_version INT NOT NULL,
  payload_hash CHAR(64) NOT NULL,
  result VARCHAR(16) NOT NULL,
  consumed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_consumed_identity (consumer_name, event_id),
  CONSTRAINT ck_notification_consumed_version CHECK (event_version > 0),
  CONSTRAINT ck_notification_consumed_result CHECK (result IN ('CONSUMED'))
);

CREATE TABLE notification_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  consumer_name VARCHAR(64) NOT NULL,
  event_id VARCHAR(36) NOT NULL,
  user_id BIGINT NOT NULL,
  booking_no VARCHAR(64) NOT NULL,
  notification_type VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL,
  body VARCHAR(512) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_record_identity (consumer_name, event_id),
  KEY idx_notification_record_user_created (user_id, created_at),
  CONSTRAINT ck_notification_record_user CHECK (user_id > 0),
  CONSTRAINT ck_notification_record_type CHECK (
    notification_type IN ('BOOKING_CONFIRMED', 'BOOKING_CANCELLED')
  )
);

CREATE TABLE notification_consume_failure (
  id BIGINT NOT NULL AUTO_INCREMENT,
  consumer_name VARCHAR(64) NOT NULL,
  event_id VARCHAR(36) NULL,
  message_fingerprint CHAR(64) NOT NULL,
  routing_key VARCHAR(96) NOT NULL,
  attempt_count INT NOT NULL,
  error_code VARCHAR(64) NOT NULL,
  terminal BOOLEAN NOT NULL,
  replay_reason VARCHAR(256) NULL,
  first_failed_at DATETIME(6) NOT NULL,
  last_failed_at DATETIME(6) NOT NULL,
  replayed_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  KEY idx_notification_failure_event (consumer_name, event_id),
  KEY idx_notification_failure_terminal (terminal, last_failed_at),
  CONSTRAINT ck_notification_failure_attempt CHECK (attempt_count >= 0)
);
