DROP DATABASE IF EXISTS stock_db;
CREATE DATABASE IF NOT EXISTS stock_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stock_db;

CREATE TABLE IF NOT EXISTS stock_ledger
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    sku_id            BIGINT UNSIGNED NOT NULL,
    on_hand_qty       INT             NOT NULL DEFAULT 0,
    reserved_qty      INT             NOT NULL DEFAULT 0,
    salable_qty       INT             NOT NULL DEFAULT 0,
    alert_threshold   INT             NOT NULL DEFAULT 0,
    status            TINYINT         NOT NULL DEFAULT 1,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_stock_ledger_sku (sku_id),
    INDEX idx_stock_ledger_status_deleted (status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_reservation
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    sub_order_no      VARCHAR(64)     NOT NULL,
    sku_id            BIGINT UNSIGNED NOT NULL,
    reserved_qty      INT             NOT NULL,
    status            VARCHAR(32)     NOT NULL,
    idempotency_key   VARCHAR(128)    NOT NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_stock_reservation_sub_sku (sub_order_no, sku_id),
    UNIQUE KEY uk_stock_reservation_idempotency (idempotency_key),
    INDEX idx_stock_reservation_sku_status_deleted (sku_id, status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_txn
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    sku_id            BIGINT UNSIGNED NOT NULL,
    sub_order_no      VARCHAR(64)     NULL,
    txn_type          VARCHAR(32)     NOT NULL,
    quantity          INT             NOT NULL,
    before_on_hand    INT             NULL,
    after_on_hand     INT             NULL,
    before_reserved   INT             NULL,
    after_reserved    INT             NULL,
    before_salable    INT             NULL,
    after_salable     INT             NULL,
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
