DROP DATABASE IF EXISTS product_db;
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE product_db;

CREATE TABLE IF NOT EXISTS category
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    parent_id        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    category_name    VARCHAR(100)    NOT NULL,
    level            TINYINT         NOT NULL COMMENT '1/2/3',
    category_path    VARCHAR(255)    NOT NULL COMMENT 'e.g. 100/200/300',
    sort_order       INT             NOT NULL DEFAULT 0,
    status           TINYINT         NOT NULL DEFAULT 1,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_category_path (category_path),
    INDEX idx_category_parent_level_deleted (parent_id, level, deleted),
    INDEX idx_category_status_deleted (status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spu
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    spu_no           VARCHAR(64)     NOT NULL,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    category_id      BIGINT UNSIGNED NOT NULL,
    title            VARCHAR(200)    NOT NULL,
    sub_title        VARCHAR(255)    NULL,
    brand_name       VARCHAR(100)    NULL,
    cover_image      VARCHAR(500)    NULL,
    detail_images    TEXT            NULL,
    attributes_json  JSON            NULL,
    sale_status      VARCHAR(32)     NOT NULL DEFAULT 'DRAFT',
    audit_status     VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_spu_no (spu_no),
    INDEX idx_spu_merchant_status_deleted (merchant_id, sale_status, deleted),
    INDEX idx_spu_category_status_deleted (category_id, sale_status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sku
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    sku_no           VARCHAR(64)     NOT NULL,
    spu_id           BIGINT UNSIGNED NOT NULL,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    category_id      BIGINT UNSIGNED NOT NULL,
    sku_code         VARCHAR(100)    NOT NULL,
    sku_name         VARCHAR(200)    NOT NULL,
    specs_json       JSON            NOT NULL,
    sale_price       DECIMAL(12, 2)  NOT NULL,
    original_price   DECIMAL(12, 2)  NULL,
    cost_price       DECIMAL(12, 2)  NULL,
    status           VARCHAR(32)     NOT NULL DEFAULT 'ON_SHELF',
    sales_count      INT             NOT NULL DEFAULT 0,
    cover_image      VARCHAR(500)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sku_no (sku_no),
    UNIQUE KEY uk_sku_code (sku_code),
    INDEX idx_sku_spu_status_deleted (spu_id, status, deleted),
    INDEX idx_sku_merchant_status_deleted (merchant_id, status, deleted),
    INDEX idx_sku_category_status_deleted (category_id, status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sku_price_history
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    sku_id           BIGINT UNSIGNED NOT NULL,
    old_price        DECIMAL(12, 2)  NOT NULL,
    new_price        DECIMAL(12, 2)  NOT NULL,
    changed_by       BIGINT UNSIGNED NULL,
    change_reason    VARCHAR(255)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_sku_price_history_sku_created_deleted (sku_id, created_at, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_review
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    spu_id           BIGINT UNSIGNED NOT NULL,
    sku_id           BIGINT UNSIGNED NOT NULL,
    order_item_id    BIGINT UNSIGNED NOT NULL,
    order_sub_no     VARCHAR(64)     NOT NULL,
    user_id          BIGINT UNSIGNED NOT NULL,
    rating           TINYINT         NOT NULL,
    review_content   TEXT            NULL,
    review_images    JSON            NULL,
    review_status    VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    merchant_reply   TEXT            NULL,
    replied_at       DATETIME        NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_product_review_order_item (order_item_id),
    INDEX idx_product_review_spu_status_deleted (spu_id, review_status, deleted),
    INDEX idx_product_review_sku_deleted (sku_id, deleted),
    INDEX idx_product_review_user_deleted (user_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outbox_event
(
    id                 BIGINT UNSIGNED PRIMARY KEY,
    event_id           VARCHAR(64)     NOT NULL,
    aggregate_type     VARCHAR(64)     NOT NULL,
    aggregate_id       VARCHAR(64)     NOT NULL,
    event_type         VARCHAR(64)     NOT NULL,
    payload            JSON            NOT NULL,
    status             VARCHAR(16)     NOT NULL DEFAULT 'NEW',
    retry_count        INT             NOT NULL DEFAULT 0,
    next_retry_at      DATETIME        NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT         NOT NULL DEFAULT 0,
    version            INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_outbox_event_id (event_id),
    INDEX idx_outbox_status_next_retry_deleted (status, next_retry_at, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inbox_consume_log
(
    id                 BIGINT UNSIGNED PRIMARY KEY,
    event_id           VARCHAR(64)     NOT NULL,
    consumer_group     VARCHAR(64)     NOT NULL,
    event_type         VARCHAR(64)     NOT NULL,
    consume_status     VARCHAR(16)     NOT NULL DEFAULT 'SUCCESS',
    error_message      VARCHAR(1000)   NULL,
    consumed_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT         NOT NULL DEFAULT 0,
    version            INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_inbox_event_group (event_id, consumer_group),
    INDEX idx_inbox_event_type_deleted (event_type, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
