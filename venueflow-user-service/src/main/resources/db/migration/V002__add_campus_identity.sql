ALTER TABLE user_profile
    ADD COLUMN campus_id VARCHAR(64) NULL AFTER display_name,
    ADD COLUMN identity_type VARCHAR(16) NOT NULL DEFAULT 'OTHER' AFTER campus_id,
    ADD COLUMN department VARCHAR(120) NULL AFTER identity_type,
    ADD COLUMN phone VARCHAR(32) NULL AFTER department,
    ADD COLUMN email VARCHAR(160) NULL AFTER phone,
    ADD CONSTRAINT uk_user_profile_campus_id UNIQUE (campus_id),
    ADD CONSTRAINT chk_user_profile_identity_type
        CHECK (identity_type IN ('STUDENT', 'STAFF', 'OTHER'));
