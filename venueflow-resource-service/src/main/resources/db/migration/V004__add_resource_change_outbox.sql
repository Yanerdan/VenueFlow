CREATE TABLE resource_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id CHAR(36) NOT NULL,
  resource_id BIGINT NOT NULL,
  aggregate_version BIGINT NOT NULL,
  event_type VARCHAR(80) NOT NULL,
  payload JSON NOT NULL,
  status VARCHAR(16) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  published_at DATETIME(6) NULL,
  last_error VARCHAR(120) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_resource_outbox_event (event_id),
  KEY idx_resource_outbox_dispatch (status, next_attempt_at, id),
  CONSTRAINT fk_resource_outbox_resource
    FOREIGN KEY (resource_id) REFERENCES resource(id)
) ENGINE=InnoDB;
