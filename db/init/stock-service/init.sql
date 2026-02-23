DROP DATABASE IF EXISTS stock_db;
CREATE DATABASE IF NOT EXISTS stock_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stock_db;

CREATE TABLE IF NOT EXISTS stock
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    product_id          BIGINT UNSIGNED NOT NULL UNIQUE,
    product_name        VARCHAR(200)    NOT NULL,
    stock_quantity      INT             NOT NULL DEFAULT 0,
    frozen_quantity     INT             NOT NULL DEFAULT 0,
    stock_status        TINYINT         NOT NULL DEFAULT 1,
    low_stock_threshold INT             NOT NULL DEFAULT 0,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    INDEX idx_stock_status (stock_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_in
(
    id         BIGINT UNSIGNED PRIMARY KEY,
    product_id BIGINT UNSIGNED NOT NULL,
    quantity   INT             NOT NULL,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    version    INT             NOT NULL DEFAULT 0,
    INDEX idx_stock_in_product (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_out
(
    id         BIGINT UNSIGNED PRIMARY KEY,
    product_id BIGINT UNSIGNED NOT NULL,
    order_id   BIGINT UNSIGNED NOT NULL,
    quantity   INT             NOT NULL,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    version    INT             NOT NULL DEFAULT 0,
    INDEX idx_stock_out_product (product_id),
    INDEX idx_stock_out_order (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_log
(
    id              BIGINT UNSIGNED PRIMARY KEY,
    product_id      BIGINT UNSIGNED NOT NULL,
    product_name    VARCHAR(200)    NULL,
    operation_type  VARCHAR(20)     NOT NULL,
    quantity_before INT             NOT NULL,
    quantity_after  INT             NOT NULL,
    quantity_change INT             NOT NULL,
    order_id        BIGINT UNSIGNED NULL,
    order_no        VARCHAR(64)     NULL,
    operator_id     BIGINT UNSIGNED NULL,
    operator_name   VARCHAR(100)    NULL,
    remark          VARCHAR(500)    NULL,
    operate_time    DATETIME        NULL,
    ip_address      VARCHAR(50)     NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    version         INT             NOT NULL DEFAULT 0,
    INDEX idx_stock_log_product_time (product_id, operate_time),
    INDEX idx_stock_log_order (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_count
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    count_no          VARCHAR(50)     NOT NULL UNIQUE,
    product_id        BIGINT UNSIGNED NOT NULL,
    product_name      VARCHAR(200)    NULL,
    expected_quantity INT             NOT NULL,
    actual_quantity   INT             NOT NULL,
    difference        INT             NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    operator_id       BIGINT UNSIGNED NULL,
    operator_name     VARCHAR(100)    NULL,
    confirm_user_id   BIGINT UNSIGNED NULL,
    confirm_user_name VARCHAR(100)    NULL,
    count_time        DATETIME        NULL,
    confirm_time      DATETIME        NULL,
    remark            VARCHAR(500)    NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    INDEX idx_stock_count_product_time (product_id, count_time),
    INDEX idx_stock_count_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
