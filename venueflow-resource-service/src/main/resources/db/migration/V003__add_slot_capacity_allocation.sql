ALTER TABLE resource_slot
    ADD COLUMN allocated_quantity INT NOT NULL DEFAULT 0 AFTER status,
    ADD CONSTRAINT chk_resource_slot_allocated_quantity_non_negative
        CHECK (allocated_quantity >= 0);

CREATE TABLE resource_slot_allocation
(
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    slot_id                 BIGINT       NOT NULL,
    operation_id            VARCHAR(64)  NOT NULL,
    operation_type          VARCHAR(16)  NOT NULL,
    quantity                INT          NOT NULL,
    request_fingerprint     CHAR(64)     NOT NULL,
    occupied_quantity_after INT          NOT NULL,
    created_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_resource_slot_allocation
        PRIMARY KEY (id),

    CONSTRAINT uk_resource_slot_allocation_operation_id
        UNIQUE (operation_id),

    CONSTRAINT fk_resource_slot_allocation_slot
        FOREIGN KEY (slot_id)
            REFERENCES resource_slot (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,

    CONSTRAINT chk_resource_slot_allocation_type
        CHECK (operation_type IN ('ALLOCATE', 'RELEASE')),

    CONSTRAINT chk_resource_slot_allocation_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT chk_resource_slot_allocation_occupied_non_negative
        CHECK (occupied_quantity_after >= 0),

    INDEX idx_resource_slot_allocation_slot_created_id
        (slot_id, created_at, id)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;
