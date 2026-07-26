CREATE TABLE booking_reconciliation_intent
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workflow_type VARCHAR(16) NOT NULL,
    request_id VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    booking_id BIGINT UNSIGNED NULL,

    slot_id BIGINT UNSIGNED NOT NULL,
    quantity INT UNSIGNED NOT NULL,

    allocation_operation_id VARCHAR(128)
           CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin NOT NULL,

    release_operation_id VARCHAR(128)
           CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin NOT NULL,

    state VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    outcome_code VARCHAR(64) NULL,
    last_error_code VARCHAR(64) NULL,

    attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
    next_check_at DATETIME(6) NULL,

    lease_owner VARCHAR(128) NULL,
    lease_expires_at DATETIME(6) NULL,

    version BIGINT UNSIGNED NOT NULL DEFAULT 0,

    resolved_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_booking_reconciliation_intent
        PRIMARY KEY (id),

    CONSTRAINT uq_booking_reconciliation_workflow_request
        UNIQUE (workflow_type, request_id),

    CONSTRAINT ck_booking_reconciliation_workflow_type
        CHECK (workflow_type IN ('ALLOCATE', 'RELEASE')),

    CONSTRAINT ck_booking_reconciliation_quantity
        CHECK (quantity > 0),

    CONSTRAINT ck_booking_reconciliation_state
        CHECK (
                state IN (
                          'OPEN',
                          'LEASED',
                          'RESOLVED',
                          'EXHAUSTED'
                )
            ),

    CONSTRAINT ck_booking_reconciliation_attempt_count
        CHECK (attempt_count <= 1000),

    CONSTRAINT ck_booking_reconciliation_lease
        CHECK (
                (
                            state = 'LEASED'
                        AND lease_owner IS NOT NULL
                        AND lease_expires_at IS NOT NULL
                    )
                OR
                (
                            state <> 'LEASED'
                        AND lease_owner IS NULL
                        AND lease_expires_at IS NULL
                    )
            ),

    CONSTRAINT ck_booking_reconciliation_terminal
        CHECK (
                (
                            state IN ('RESOLVED', 'EXHAUSTED')
                        AND outcome_code IS NOT NULL
                        AND resolved_at IS NOT NULL
                        AND next_check_at IS NULL
                    )
                OR
                (
                            state NOT IN ('RESOLVED', 'EXHAUSTED')
                        AND resolved_at IS NULL
                        AND next_check_at IS NOT NULL
                    )
            ),

    INDEX idx_booking_reconciliation_due
        (state, next_check_at, lease_expires_at, id),

    INDEX idx_booking_reconciliation_booking
        (booking_id, workflow_type),

    INDEX idx_booking_reconciliation_allocation_operation
        (slot_id, allocation_operation_id),

    INDEX idx_booking_reconciliation_release_operation
        (slot_id, release_operation_id),

    INDEX idx_booking_reconciliation_retention
        (state, resolved_at, id)
);


CREATE TABLE reconciliation_run
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    run_key VARCHAR(64)
           CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin NOT NULL,

    trigger_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',

    owner_id VARCHAR(128) NOT NULL,
    lease_expires_at DATETIME(6) NOT NULL,

    operator_reason VARCHAR(256) NULL,

    claimed_count INT UNSIGNED NOT NULL DEFAULT 0,
    consistent_count INT UNSIGNED NOT NULL DEFAULT 0,
    repaired_count INT UNSIGNED NOT NULL DEFAULT 0,
    unresolved_count INT UNSIGNED NOT NULL DEFAULT 0,
    failed_count INT UNSIGNED NOT NULL DEFAULT 0,
    lease_reclaimed_count INT UNSIGNED NOT NULL DEFAULT 0,

    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,

    CONSTRAINT pk_reconciliation_run
        PRIMARY KEY (id),

    CONSTRAINT uq_reconciliation_run_key
        UNIQUE (run_key),

    CONSTRAINT ck_reconciliation_run_trigger
        CHECK (trigger_type IN ('SCHEDULED', 'OPERATOR')),

    CONSTRAINT ck_reconciliation_run_status
        CHECK (
                status IN (
                           'RUNNING',
                           'COMPLETED',
                           'FAILED',
                           'INTERRUPTED'
                )
            ),

    CONSTRAINT ck_reconciliation_run_completion
        CHECK (
                (
                            status = 'RUNNING'
                        AND completed_at IS NULL
                    )
                OR
                (
                            status <> 'RUNNING'
                        AND completed_at IS NOT NULL
                    )
            ),

    INDEX idx_reconciliation_run_status
        (status, lease_expires_at, id),

    INDEX idx_reconciliation_run_retention
        (started_at, id)
);


CREATE TABLE reconciliation_issue
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    intent_id BIGINT UNSIGNED NOT NULL,

    issue_code VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'OPEN',

    occurrence_count INT UNSIGNED NOT NULL DEFAULT 1,

    first_seen_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_seen_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at DATETIME(6) NULL,

    CONSTRAINT pk_reconciliation_issue
        PRIMARY KEY (id),

    CONSTRAINT uq_reconciliation_issue
        UNIQUE (intent_id, issue_code),

    CONSTRAINT fk_reconciliation_issue_intent
        FOREIGN KEY (intent_id)
            REFERENCES booking_reconciliation_intent (id),

    CONSTRAINT ck_reconciliation_issue_severity
        CHECK (
                severity IN (
                             'WARNING',
                             'ERROR',
                             'CRITICAL'
                )
            ),

    CONSTRAINT ck_reconciliation_issue_state
        CHECK (state IN ('OPEN', 'RESOLVED')),

    CONSTRAINT ck_reconciliation_issue_occurrence
        CHECK (occurrence_count > 0),

    CONSTRAINT ck_reconciliation_issue_resolution
        CHECK (
                (
                            state = 'OPEN'
                        AND resolved_at IS NULL
                    )
                OR
                (
                            state = 'RESOLVED'
                        AND resolved_at IS NOT NULL
                    )
            ),

    INDEX idx_reconciliation_issue_open
        (state, severity, last_seen_at, id),

    INDEX idx_reconciliation_issue_retention
        (resolved_at, id)
);


CREATE TABLE repair_action
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    intent_id BIGINT UNSIGNED NOT NULL,
    run_id BIGINT UNSIGNED NOT NULL,

    attempt_number INT UNSIGNED NOT NULL,

    action_type VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    operation_id VARCHAR(128)
           CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin NOT NULL,

    status VARCHAR(16) NOT NULL DEFAULT 'STARTED',
    result_code VARCHAR(64) NULL,

    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,

    CONSTRAINT pk_repair_action
        PRIMARY KEY (id),

    CONSTRAINT uq_repair_action_attempt
        UNIQUE (
                intent_id,
                action_type,
                attempt_number
            ),

    CONSTRAINT fk_repair_action_intent
        FOREIGN KEY (intent_id)
            REFERENCES booking_reconciliation_intent (id),

    CONSTRAINT fk_repair_action_run
        FOREIGN KEY (run_id)
            REFERENCES reconciliation_run (id),

    CONSTRAINT ck_repair_action_attempt_number
        CHECK (attempt_number > 0),

    CONSTRAINT ck_repair_action_type
        CHECK (
                action_type IN (
                                'RELEASE_ORPHAN',
                                'COMPLETE_CANCELLATION'
                )
            ),

    CONSTRAINT ck_repair_action_status
        CHECK (
                status IN (
                           'STARTED',
                           'SUCCEEDED',
                           'FAILED',
                           'UNKNOWN'
                )
            ),

    CONSTRAINT ck_repair_action_completion
        CHECK (
                (
                            status = 'STARTED'
                        AND result_code IS NULL
                        AND completed_at IS NULL
                    )
                OR
                (
                            status <> 'STARTED'
                        AND result_code IS NOT NULL
                        AND completed_at IS NOT NULL
                    )
            ),

    INDEX idx_repair_action_intent
        (intent_id, started_at, id),

    INDEX idx_repair_action_run
        (run_id, started_at, id),

    INDEX idx_repair_action_retention
        (started_at, id)
);
