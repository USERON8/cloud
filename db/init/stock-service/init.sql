DROP DATABASE IF EXISTS stock_db;
CREATE DATABASE IF NOT EXISTS stock_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stock_db;

CREATE TABLE IF NOT EXISTS stock_ledger
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    sku_id              BIGINT UNSIGNED NOT NULL,
    sku_code            VARCHAR(100)    NOT NULL,
    sku_name            VARCHAR(200)    NOT NULL,
    on_hand_qty         INT             NOT NULL DEFAULT 0,
    reserved_qty        INT             NOT NULL DEFAULT 0,
    salable_qty         INT             NOT NULL DEFAULT 0,
    warning_threshold   INT             NOT NULL DEFAULT 0,
    stock_status        VARCHAR(32)     NOT NULL DEFAULT 'NORMAL',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_stock_ledger_sku_id (sku_id),
    UNIQUE KEY uk_stock_ledger_sku_code (sku_code),
    INDEX idx_stock_ledger_status_deleted (stock_status, deleted),
    INDEX idx_stock_ledger_salable_deleted (salable_qty, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_reservation
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    reservation_no      VARCHAR(64)     NOT NULL,
    main_order_no       VARCHAR(64)     NOT NULL,
    sub_order_no        VARCHAR(64)     NOT NULL,
    sku_id              BIGINT UNSIGNED NOT NULL,
    reserved_qty        INT             NOT NULL,
    reservation_status  VARCHAR(32)     NOT NULL DEFAULT 'RESERVED',
    expire_at           DATETIME        NULL,
    released_at         DATETIME        NULL,
    released_reason     VARCHAR(255)    NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_stock_reservation_no (reservation_no),
    UNIQUE KEY uk_stock_reservation_sub_order_sku (sub_order_no, sku_id),
    INDEX idx_stock_reservation_main_status_deleted (main_order_no, reservation_status, deleted),
    INDEX idx_stock_reservation_expire_deleted (expire_at, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_txn
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    txn_no              VARCHAR(64)     NOT NULL,
    sku_id              BIGINT UNSIGNED NOT NULL,
    main_order_no       VARCHAR(64)     NULL,
    sub_order_no        VARCHAR(64)     NULL,
    txn_type            VARCHAR(32)     NOT NULL COMMENT 'IN/RESERVE/RELEASE/DEDUCT/ROLLBACK',
    qty                 INT             NOT NULL,
    before_on_hand      INT             NOT NULL,
    before_reserved     INT             NOT NULL,
    before_salable      INT             NOT NULL,
    after_on_hand       INT             NOT NULL,
    after_reserved      INT             NOT NULL,
    after_salable       INT             NOT NULL,
    operator_id         BIGINT UNSIGNED NULL,
    remark              VARCHAR(500)    NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_stock_txn_no (txn_no),
    INDEX idx_stock_txn_sku_created_deleted (sku_id, created_at, deleted),
    INDEX idx_stock_txn_order_created_deleted (sub_order_no, created_at, deleted)
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
