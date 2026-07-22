CREATE TABLE resource_slot
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    resource_id BIGINT       NOT NULL,
    start_at   DATETIME(3)  NOT NULL,
    end_at     DATETIME(3)  NOT NULL,
    status     VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    version    BIGINT       NOT NULL DEFAULT 1,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_resource_slot
        PRIMARY KEY (id),

    CONSTRAINT uk_resource_slot_resource_time_range
        UNIQUE (resource_id, start_at, end_at),

    CONSTRAINT fk_resource_slot_resource
        FOREIGN KEY (resource_id)
            REFERENCES resource (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,

    CONSTRAINT chk_resource_slot_time_range
        CHECK (end_at > start_at),

    CONSTRAINT chk_resource_slot_status
        CHECK (status IN ('OPEN', 'CLOSED')),

    CONSTRAINT chk_resource_slot_version_positive
        CHECK (version >= 1),

    INDEX idx_resource_slot_resource_start_id
        (resource_id, start_at, id),

    INDEX idx_resource_slot_resource_end_at
        (resource_id, end_at)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;
