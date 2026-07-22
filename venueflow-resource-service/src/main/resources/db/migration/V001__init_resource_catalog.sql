CREATE TABLE resource_category
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(64)  NOT NULL,
    name       VARCHAR(128) NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_resource_category
        PRIMARY KEY (id),

    CONSTRAINT uk_resource_category_code
        UNIQUE (code),

    CONSTRAINT chk_resource_category_code_not_blank
        CHECK (CHAR_LENGTH(TRIM(code)) > 0),

    CONSTRAINT chk_resource_category_name_not_blank
        CHECK (CHAR_LENGTH(TRIM(name)) > 0)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;


CREATE TABLE resource
(
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    resource_no VARCHAR(64)   NOT NULL,
    category_id BIGINT        NOT NULL,
    name        VARCHAR(128)  NOT NULL,
    description VARCHAR(1000) NULL,
    location    VARCHAR(255)  NULL,
    capacity    INT           NOT NULL,
    status      VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    version     BIGINT        NOT NULL DEFAULT 1,
    created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_resource
        PRIMARY KEY (id),

    CONSTRAINT uk_resource_resource_no
        UNIQUE (resource_no),

    CONSTRAINT fk_resource_category
        FOREIGN KEY (category_id)
            REFERENCES resource_category (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,

    CONSTRAINT chk_resource_no_not_blank
        CHECK (CHAR_LENGTH(TRIM(resource_no)) > 0),

    CONSTRAINT chk_resource_name_not_blank
        CHECK (CHAR_LENGTH(TRIM(name)) > 0),

    CONSTRAINT chk_resource_capacity_positive
        CHECK (capacity > 0),

    CONSTRAINT chk_resource_status
        CHECK (
                status IN (
                           'DRAFT',
                           'ACTIVE',
                           'SUSPENDED',
                           'ARCHIVED'
                )
            ),

    CONSTRAINT chk_resource_version_positive
        CHECK (version >= 1),

    INDEX idx_resource_category_status_id
        (category_id, status, id),

    INDEX idx_resource_status_id
        (status, id)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;
