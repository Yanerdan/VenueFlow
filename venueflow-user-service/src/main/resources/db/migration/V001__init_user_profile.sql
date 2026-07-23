CREATE TABLE user_profile
(
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    external_user_id    VARCHAR(128)
                            CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin
                                     NOT NULL,
    display_name        VARCHAR(120) NOT NULL,
    account_status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    booking_eligibility VARCHAR(16)  NOT NULL DEFAULT 'ELIGIBLE',
    version             BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_user_profile
        PRIMARY KEY (id),

    CONSTRAINT uk_user_profile_external_user_id
        UNIQUE (external_user_id),

    CONSTRAINT chk_user_profile_external_user_id
        CHECK (
                CHAR_LENGTH(TRIM(external_user_id)) BETWEEN 1 AND 128
                AND external_user_id = TRIM(external_user_id)
            ),

    CONSTRAINT chk_user_profile_display_name
        CHECK (
            CHAR_LENGTH(TRIM(display_name)) BETWEEN 1 AND 120
            ),

    CONSTRAINT chk_user_profile_account_status
        CHECK (
                account_status IN ('ACTIVE', 'SUSPENDED')
            ),

    CONSTRAINT chk_user_profile_booking_eligibility
        CHECK (
                booking_eligibility IN ('ELIGIBLE', 'INELIGIBLE')
            ),

    INDEX idx_user_profile_booking_state
        (account_status, booking_eligibility)
)
    ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;
