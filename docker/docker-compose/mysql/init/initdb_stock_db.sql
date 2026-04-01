CREATE DATABASE IF NOT EXISTS stock_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stock_db;

CREATE TABLE IF NOT EXISTS stock_segment
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    sku_id            BIGINT UNSIGNED NOT NULL,
    segment_id        INT             NOT NULL,
    available_qty     INT             NOT NULL DEFAULT 0,
    locked_qty        INT             NOT NULL DEFAULT 0,
    sold_qty          INT             NOT NULL DEFAULT 0,
    alert_threshold   INT             NOT NULL DEFAULT 0,
    status            TINYINT         NOT NULL DEFAULT 1,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_stock_segment_sku_segment (sku_id, segment_id),
    INDEX idx_stock_segment_sku_deleted (sku_id, deleted),
    INDEX idx_stock_segment_sku_available (sku_id, available_qty, deleted),
    INDEX idx_stock_segment_status_deleted (status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_reservation
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    main_order_no     VARCHAR(64)     NULL,
    sub_order_no      VARCHAR(64)     NOT NULL,
    sku_id            BIGINT UNSIGNED NOT NULL,
    segment_id        INT             NOT NULL,
    quantity          INT             NOT NULL,
    status            VARCHAR(32)     NOT NULL,
    idempotency_key   VARCHAR(128)    NOT NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_stock_reservation_sub_sku_segment (sub_order_no, sku_id, segment_id),
    UNIQUE KEY uk_stock_reservation_idempotency (idempotency_key),
    INDEX idx_stock_reservation_sub_sku_deleted (sub_order_no, sku_id, deleted),
    INDEX idx_stock_reservation_sku_status_deleted (sku_id, status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_txn
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    sku_id            BIGINT UNSIGNED NOT NULL,
    segment_id        INT             NULL,
    sub_order_no      VARCHAR(64)     NULL,
    txn_type          VARCHAR(32)     NOT NULL,
    quantity          INT             NOT NULL,
    before_available  INT             NULL,
    after_available   INT             NULL,
    before_locked     INT             NULL,
    after_locked      INT             NULL,
    before_sold       INT             NULL,
    after_sold        INT             NULL,
    remark            VARCHAR(1000)   NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    INDEX idx_stock_txn_sku_type_deleted (sku_id, txn_type, deleted),
    INDEX idx_stock_txn_sub_deleted (sub_order_no, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
