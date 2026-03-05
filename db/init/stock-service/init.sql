DROP DATABASE IF EXISTS stock_db;
CREATE DATABASE IF NOT EXISTS stock_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stock_db;

CREATE TABLE IF NOT EXISTS stock
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    product_id          BIGINT UNSIGNED NOT NULL,
    product_name        VARCHAR(200)    NULL,
    stock_quantity      INT             NOT NULL DEFAULT 0,
    frozen_quantity     INT             NOT NULL DEFAULT 0,
    stock_status        INT             NOT NULL DEFAULT 1,
    low_stock_threshold INT             NOT NULL DEFAULT 0,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_stock_product_id (product_id),
    INDEX idx_stock_status_deleted (stock_status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_in
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    product_id          BIGINT UNSIGNED NOT NULL,
    quantity            INT             NOT NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    INDEX idx_stock_in_product_deleted (product_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_out
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    product_id          BIGINT UNSIGNED NOT NULL,
    order_id            BIGINT UNSIGNED NULL,
    quantity            INT             NOT NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    INDEX idx_stock_out_product_deleted (product_id, deleted),
    INDEX idx_stock_out_order_deleted (order_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_count
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    count_no            VARCHAR(64)     NOT NULL,
    product_id          BIGINT UNSIGNED NOT NULL,
    product_name        VARCHAR(200)    NULL,
    expected_quantity   INT             NOT NULL DEFAULT 0,
    actual_quantity     INT             NULL,
    difference          INT             NULL,
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    operator_id         BIGINT UNSIGNED NULL,
    operator_name       VARCHAR(100)    NULL,
    confirm_user_id     BIGINT UNSIGNED NULL,
    confirm_user_name   VARCHAR(100)    NULL,
    count_time          DATETIME        NULL,
    confirm_time        DATETIME        NULL,
    remark              VARCHAR(1000)   NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_stock_count_no (count_no),
    INDEX idx_stock_count_product_deleted (product_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_log
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    product_id          BIGINT UNSIGNED NOT NULL,
    product_name        VARCHAR(200)    NULL,
    operation_type      VARCHAR(32)     NOT NULL,
    quantity_before     INT             NULL,
    quantity_after      INT             NULL,
    quantity_change     INT             NULL,
    order_id            BIGINT UNSIGNED NULL,
    order_no            VARCHAR(64)     NULL,
    operator_id         BIGINT UNSIGNED NULL,
    operator_name       VARCHAR(100)    NULL,
    remark              VARCHAR(1000)   NULL,
    operate_time        DATETIME        NULL,
    ip_address          VARCHAR(64)     NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    INDEX idx_stock_log_product_deleted (product_id, deleted),
    INDEX idx_stock_log_order_deleted (order_id, deleted)
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
