DROP DATABASE IF EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS payment_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE payment_db;

CREATE TABLE IF NOT EXISTS payment_order
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    payment_no          VARCHAR(64)     NOT NULL,
    main_order_no       VARCHAR(64)     NOT NULL,
    sub_order_no        VARCHAR(64)     NULL,
    user_id             BIGINT UNSIGNED NOT NULL,
    payment_status      VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    payment_channel     VARCHAR(32)     NOT NULL DEFAULT 'ALIPAY',
    total_amount        DECIMAL(12, 2)  NOT NULL,
    paid_amount         DECIMAL(12, 2)  NULL,
    paid_at             DATETIME        NULL,
    transaction_no      VARCHAR(128)    NULL,
    trace_id            VARCHAR(64)     NULL,
    idempotency_key     VARCHAR(128)    NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_order_no (payment_no),
    UNIQUE KEY uk_payment_order_idempotency_key (idempotency_key),
    INDEX idx_payment_order_main_status_deleted (main_order_no, payment_status, deleted),
    INDEX idx_payment_order_user_status_deleted (user_id, payment_status, deleted),
    INDEX idx_payment_order_trace_deleted (trace_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_refund
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    refund_payment_no   VARCHAR(64)     NOT NULL,
    after_sale_no       VARCHAR(64)     NOT NULL,
    payment_no          VARCHAR(64)     NOT NULL,
    main_order_no       VARCHAR(64)     NOT NULL,
    sub_order_no        VARCHAR(64)     NULL,
    user_id             BIGINT UNSIGNED NOT NULL,
    refund_status       VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    refund_amount       DECIMAL(12, 2)  NOT NULL,
    refund_channel      VARCHAR(32)     NOT NULL DEFAULT 'ALIPAY',
    refund_transaction_no VARCHAR(128)  NULL,
    refunded_at         DATETIME        NULL,
    reason              VARCHAR(255)    NULL,
    trace_id            VARCHAR(64)     NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_refund_no (refund_payment_no),
    INDEX idx_payment_refund_after_sale_status_deleted (after_sale_no, refund_status, deleted),
    INDEX idx_payment_refund_payment_no_deleted (payment_no, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_callback_log
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    callback_id         VARCHAR(64)     NOT NULL,
    callback_type       VARCHAR(32)     NOT NULL COMMENT 'PAY|REFUND',
    source_channel      VARCHAR(32)     NOT NULL,
    business_no         VARCHAR(128)    NOT NULL,
    callback_payload    JSON            NOT NULL,
    callback_status     VARCHAR(16)     NOT NULL DEFAULT 'NEW',
    callback_time       DATETIME        NOT NULL,
    processed_time      DATETIME        NULL,
    error_message       VARCHAR(1000)   NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_callback_id (callback_id),
    INDEX idx_payment_callback_business_deleted (business_no, deleted),
    INDEX idx_payment_callback_status_deleted (callback_status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    order_id            BIGINT UNSIGNED NOT NULL,
    user_id             BIGINT UNSIGNED NOT NULL,
    amount              DECIMAL(12, 2)  NOT NULL,
    status              INT             NOT NULL DEFAULT 0,
    channel             INT             NOT NULL DEFAULT 1,
    transaction_id      VARCHAR(128)    NULL,
    trace_id            VARCHAR(64)     NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    INDEX idx_payment_order_deleted (order_id, deleted),
    INDEX idx_payment_user_status_deleted (user_id, status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_flow
(
    id                  BIGINT UNSIGNED PRIMARY KEY,
    payment_id          BIGINT UNSIGNED NOT NULL,
    flow_type           INT             NOT NULL,
    amount              DECIMAL(12, 2)  NOT NULL,
    trace_id            VARCHAR(64)     NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    version             INT             NOT NULL DEFAULT 0,
    INDEX idx_payment_flow_payment_deleted (payment_id, deleted)
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
