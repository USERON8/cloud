DROP DATABASE IF EXISTS order_db;
CREATE DATABASE IF NOT EXISTS order_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE order_db;

CREATE TABLE IF NOT EXISTS orders
(
    id            BIGINT UNSIGNED PRIMARY KEY,
    order_no      VARCHAR(64)     NULL UNIQUE,
    user_id       BIGINT UNSIGNED NOT NULL,
    total_amount  DECIMAL(10, 2)  NOT NULL,
    pay_amount    DECIMAL(10, 2)  NOT NULL,
    status        TINYINT         NOT NULL,
    refund_status TINYINT         NULL DEFAULT 0,
    address_id    BIGINT UNSIGNED NOT NULL,
    pay_time      DATETIME        NULL,
    ship_time     DATETIME        NULL,
    complete_time DATETIME        NULL,
    cancel_time   DATETIME        NULL,
    cancel_reason VARCHAR(255)    NULL,
    remark        VARCHAR(255)    NULL,
    shop_id       BIGINT UNSIGNED NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    version       INT             NOT NULL DEFAULT 0,
    INDEX idx_order_user_status (user_id, status),
    INDEX idx_order_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_item
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    order_id         BIGINT UNSIGNED NOT NULL,
    product_id       BIGINT UNSIGNED NOT NULL,
    product_snapshot JSON            NOT NULL,
    quantity         INT             NOT NULL,
    price            DECIMAL(10, 2)  NOT NULL,
    create_by        BIGINT UNSIGNED NULL,
    update_by        BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_order_item_order (order_id),
    INDEX idx_order_item_product (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refunds
(
    id                    BIGINT UNSIGNED PRIMARY KEY,
    refund_no             VARCHAR(64)     NOT NULL UNIQUE,
    order_id              BIGINT UNSIGNED NOT NULL,
    order_no              VARCHAR(64)     NOT NULL,
    user_id               BIGINT UNSIGNED NOT NULL,
    merchant_id           BIGINT UNSIGNED NULL,
    refund_type           TINYINT         NOT NULL DEFAULT 1,
    refund_reason         VARCHAR(255)    NOT NULL,
    refund_description    TEXT            NULL,
    refund_amount         DECIMAL(10, 2)  NOT NULL,
    refund_quantity       INT             NOT NULL DEFAULT 1,
    status                TINYINT         NOT NULL DEFAULT 0,
    audit_time            DATETIME        NULL,
    audit_remark          VARCHAR(500)    NULL,
    logistics_company     VARCHAR(100)    NULL,
    logistics_no          VARCHAR(100)    NULL,
    refund_time           DATETIME        NULL,
    refund_channel        VARCHAR(50)     NULL,
    refund_transaction_no VARCHAR(100)    NULL,
    created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted            TINYINT         NOT NULL DEFAULT 0,
    INDEX idx_refund_order_id (order_id),
    INDEX idx_refund_user_id (user_id),
    INDEX idx_refund_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
