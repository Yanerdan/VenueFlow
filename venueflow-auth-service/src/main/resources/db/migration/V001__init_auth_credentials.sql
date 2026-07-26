CREATE TABLE auth_credentials (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id CHAR(36) NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until DATETIME(6) NULL,
  token_version BIGINT NOT NULL DEFAULT 1,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_credentials_user_id (user_id),
  UNIQUE KEY uk_auth_credentials_username (username),
  CONSTRAINT chk_auth_credentials_failed_attempts CHECK (failed_attempts >= 0),
  CONSTRAINT chk_auth_credentials_token_version CHECK (token_version > 0),
  CONSTRAINT chk_auth_credentials_version CHECK (version >= 0)
) ENGINE=InnoDB;

CREATE TABLE auth_refresh_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  token_hash CHAR(64) NOT NULL,
  user_id CHAR(36) NOT NULL,
  username VARCHAR(64) NOT NULL,
  token_version BIGINT NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6) NULL,
  replaced_by_hash CHAR(64) NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_refresh_tokens_hash (token_hash),
  KEY idx_auth_refresh_tokens_user (user_id, expires_at),
  CONSTRAINT chk_auth_refresh_tokens_token_version CHECK (token_version > 0)
) ENGINE=InnoDB;
