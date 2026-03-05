DROP DATABASE IF EXISTS product_db;
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE product_db;

CREATE TABLE IF NOT EXISTS category
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    parent_id        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    name             VARCHAR(100)    NOT NULL,
    level            TINYINT         NOT NULL,
    path             VARCHAR(255)    NOT NULL,
    sort_order       INT             NOT NULL DEFAULT 0,
    status           TINYINT         NOT NULL DEFAULT 1,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    CONSTRAINT chk_category_level CHECK (level IN (1, 2, 3)),
    INDEX idx_category_parent_deleted (parent_id, deleted),
    INDEX idx_category_level_deleted (level, deleted),
    INDEX idx_category_path_deleted (path, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spu
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    spu_name         VARCHAR(200)    NOT NULL,
    subtitle         VARCHAR(255)    NULL,
    category_id      BIGINT UNSIGNED NOT NULL,
    brand_id         BIGINT UNSIGNED NULL,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    status           TINYINT         NOT NULL DEFAULT 1,
    description      TEXT            NULL,
    main_image       VARCHAR(500)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_spu_category_status_deleted (category_id, status, deleted),
    INDEX idx_spu_merchant_deleted (merchant_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sku
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    spu_id           BIGINT UNSIGNED NOT NULL,
    sku_code         VARCHAR(100)    NOT NULL,
    sku_name         VARCHAR(200)    NOT NULL,
    spec_json        JSON            NULL,
    sale_price       DECIMAL(12, 2)  NOT NULL,
    market_price     DECIMAL(12, 2)  NULL,
    cost_price       DECIMAL(12, 2)  NULL,
    status           TINYINT         NOT NULL DEFAULT 1,
    image_url        VARCHAR(500)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sku_code (sku_code),
    INDEX idx_sku_spu_status_deleted (spu_id, status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_review
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    spu_id           BIGINT UNSIGNED NOT NULL,
    sku_id           BIGINT UNSIGNED NOT NULL,
    order_sub_no     VARCHAR(64)     NOT NULL,
    user_id          BIGINT UNSIGNED NOT NULL,
    rating           TINYINT         NOT NULL,
    content          TEXT            NULL,
    images           JSON            NULL,
    tags             VARCHAR(500)    NULL,
    is_anonymous     TINYINT         NOT NULL DEFAULT 0,
    audit_status     VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    merchant_reply   TEXT            NULL,
    reply_time       DATETIME        NULL,
    like_count       INT             NOT NULL DEFAULT 0,
    is_visible       TINYINT         NOT NULL DEFAULT 1,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_review_order_user (order_sub_no, user_id),
    INDEX idx_review_spu_status_deleted (spu_id, audit_status, deleted),
    INDEX idx_review_sku_deleted (sku_id, deleted),
    INDEX idx_review_user_deleted (user_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outbox_event
(
    id                 BIGINT UNSIGNED PRIMARY KEY,
    event_id           VARCHAR(64)   NOT NULL,
    aggregate_type     VARCHAR(64)   NOT NULL,
    aggregate_id       VARCHAR(64)   NOT NULL,
    event_type         VARCHAR(64)   NOT NULL,
    payload            JSON          NOT NULL,
    status             VARCHAR(16)   NOT NULL DEFAULT 'NEW',
    retry_count        INT           NOT NULL DEFAULT 0,
    next_retry_at      DATETIME      NULL,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT       NOT NULL DEFAULT 0,
    version            INT           NOT NULL DEFAULT 0,
    UNIQUE KEY uk_outbox_event_id (event_id),
    INDEX idx_outbox_status_next_retry_deleted (status, next_retry_at, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inbox_consume_log
(
    id                 BIGINT UNSIGNED PRIMARY KEY,
    event_id           VARCHAR(64)   NOT NULL,
    consumer_group     VARCHAR(64)   NOT NULL,
    event_type         VARCHAR(64)   NOT NULL,
    consume_status     VARCHAR(16)   NOT NULL DEFAULT 'SUCCESS',
    error_message      VARCHAR(1000) NULL,
    consumed_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT       NOT NULL DEFAULT 0,
    version            INT           NOT NULL DEFAULT 0,
    UNIQUE KEY uk_inbox_event_group (event_id, consumer_group),
    INDEX idx_inbox_event_type_deleted (event_type, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
