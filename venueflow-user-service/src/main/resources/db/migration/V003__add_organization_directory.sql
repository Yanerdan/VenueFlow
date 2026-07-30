ALTER TABLE user_profile
  ADD COLUMN authoritative_source VARCHAR(48) NULL AFTER email,
  ADD COLUMN organization_external_key VARCHAR(96) NULL AFTER authoritative_source,
  ADD COLUMN directory_synced_at DATETIME(6) NULL AFTER organization_external_key;

CREATE TABLE organization_unit (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source VARCHAR(48) NOT NULL,
  external_key VARCHAR(96) NOT NULL,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(160) NOT NULL,
  parent_external_key VARCHAR(96) NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  last_synced_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_organization_unit_source_key (source, external_key),
  KEY idx_organization_unit_parent (source, parent_external_key),
  KEY idx_organization_unit_active (source, active)
) ENGINE=InnoDB;

CREATE TABLE directory_membership (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source VARCHAR(48) NOT NULL,
  external_user_id VARCHAR(128) NOT NULL,
  organization_external_key VARCHAR(96) NOT NULL,
  campus_id VARCHAR(64) NULL,
  identity_type VARCHAR(16) NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  last_synced_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_directory_membership_source_user (source, external_user_id),
  KEY idx_directory_membership_unit (source, organization_external_key),
  CONSTRAINT chk_directory_membership_identity
    CHECK (identity_type IN ('STUDENT', 'STAFF', 'OTHER'))
) ENGINE=InnoDB;

CREATE TABLE directory_sync_run (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source VARCHAR(48) NOT NULL,
  run_key VARCHAR(96) NOT NULL,
  sync_mode VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL,
  organization_count INT NOT NULL DEFAULT 0,
  membership_count INT NOT NULL DEFAULT 0,
  error_summary VARCHAR(500) NULL,
  started_at DATETIME(6) NOT NULL,
  completed_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_directory_sync_run_source_key (source, run_key),
  KEY idx_directory_sync_run_started (source, started_at),
  CONSTRAINT chk_directory_sync_run_mode CHECK (sync_mode IN ('PARTIAL', 'FULL')),
  CONSTRAINT chk_directory_sync_run_status
    CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED'))
) ENGINE=InnoDB;
