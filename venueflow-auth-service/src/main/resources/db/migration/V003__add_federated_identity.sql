ALTER TABLE auth_credentials
  MODIFY COLUMN password_hash VARCHAR(100) NULL,
  ADD COLUMN credential_source VARCHAR(16) NOT NULL DEFAULT 'LOCAL' AFTER username,
  ADD CONSTRAINT chk_auth_credentials_source
    CHECK (credential_source IN ('LOCAL', 'OIDC'));

CREATE TABLE auth_external_identities (
  id BIGINT NOT NULL AUTO_INCREMENT,
  provider_key VARCHAR(48) NOT NULL,
  issuer VARCHAR(255) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  user_id CHAR(36) NOT NULL,
  username VARCHAR(64) NOT NULL,
  campus_id VARCHAR(64) NULL,
  created_at DATETIME(6) NOT NULL,
  last_login_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_external_identity_subject (issuer, subject),
  UNIQUE KEY uk_auth_external_identity_provider_user (provider_key, user_id),
  KEY idx_auth_external_identity_campus (campus_id)
) ENGINE=InnoDB;

CREATE TABLE auth_oidc_transactions (
  state_hash CHAR(64) NOT NULL,
  provider_key VARCHAR(48) NOT NULL,
  nonce VARCHAR(96) NOT NULL,
  code_verifier VARCHAR(128) NOT NULL,
  redirect_uri VARCHAR(500) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  consumed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (state_hash),
  KEY idx_auth_oidc_transaction_expiry (expires_at)
) ENGINE=InnoDB;

CREATE TABLE auth_login_completions (
  code_hash CHAR(64) NOT NULL,
  user_id CHAR(36) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  consumed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (code_hash),
  KEY idx_auth_login_completion_expiry (expires_at)
) ENGINE=InnoDB;
