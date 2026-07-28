ALTER TABLE resource
    ADD COLUMN booking_notice VARCHAR(1000) NULL AFTER final_approver_external_user_id,
    ADD COLUMN min_advance_hours INT NOT NULL DEFAULT 0 AFTER booking_notice,
    ADD COLUMN max_advance_days INT NOT NULL DEFAULT 90 AFTER min_advance_hours,
    ADD COLUMN max_duration_minutes INT NOT NULL DEFAULT 480 AFTER max_advance_days,
    ADD CONSTRAINT chk_resource_min_advance_hours CHECK (min_advance_hours BETWEEN 0 AND 720),
    ADD CONSTRAINT chk_resource_max_advance_days CHECK (max_advance_days BETWEEN 1 AND 365),
    ADD CONSTRAINT chk_resource_max_duration_minutes CHECK (max_duration_minutes BETWEEN 15 AND 1440);
