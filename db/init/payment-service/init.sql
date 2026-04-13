DROP DATABASE IF EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS payment_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE payment_db;

CREATE TABLE IF NOT EXISTS payment_order
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    payment_no        VARCHAR(64)     NOT NULL,
    main_order_no     VARCHAR(64)     NOT NULL,
    sub_order_no      VARCHAR(64)     NOT NULL,
    user_id           BIGINT UNSIGNED NOT NULL,
    amount            DECIMAL(12, 2)  NOT NULL,
    provider          VARCHAR(32)     NULL,
    provider_app_id   VARCHAR(64)     NULL,
    provider_merchant_id VARCHAR(64)  NULL,
    biz_type          VARCHAR(32)     NULL,
    biz_order_key     VARCHAR(128)    NULL,
    channel           VARCHAR(32)     NOT NULL,
    status            VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    provider_txn_no   VARCHAR(128)    NULL,
    idempotency_key   VARCHAR(128)    NOT NULL,
    paid_at           DATETIME        NULL,
    poll_count        INT             NOT NULL DEFAULT 0,
    next_poll_at      DATETIME        NULL,
    last_polled_at    DATETIME        NULL,
    last_poll_error   VARCHAR(255)    NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_order_no (payment_no),
    UNIQUE KEY uk_payment_order_idem (idempotency_key),
    INDEX idx_payment_order_main_sub_deleted (main_order_no, sub_order_no, deleted),
    INDEX idx_payment_order_user_status_deleted (user_id, status, deleted),
    INDEX idx_payment_order_provider_deleted (provider, deleted),
    INDEX idx_payment_order_provider_txn_deleted (provider_txn_no, deleted),
    INDEX idx_payment_order_biz_key_deleted (biz_type, biz_order_key, deleted),
    INDEX idx_payment_order_status_poll_deleted (status, next_poll_at, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_refund
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    refund_no         VARCHAR(64)     NOT NULL,
    payment_no        VARCHAR(64)     NOT NULL,
    provider          VARCHAR(32)     NULL,
    provider_app_id   VARCHAR(64)     NULL,
    provider_merchant_id VARCHAR(64)  NULL,
    after_sale_no     VARCHAR(64)     NOT NULL,
    refund_amount     DECIMAL(12, 2)  NOT NULL,
    status            VARCHAR(32)     NOT NULL DEFAULT 'REFUNDING',
    reason            VARCHAR(255)    NOT NULL,
    idempotency_key   VARCHAR(128)    NOT NULL,
    refunded_at       DATETIME        NULL,
    retry_count       INT             NOT NULL DEFAULT 0,
    next_retry_at     DATETIME        NULL,
    last_retry_at     DATETIME        NULL,
    last_error        VARCHAR(255)    NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_refund_no (refund_no),
    UNIQUE KEY uk_payment_refund_idem (idempotency_key),
    INDEX idx_payment_refund_payment_deleted (payment_no, deleted),
    INDEX idx_payment_refund_after_sale_deleted (after_sale_no, deleted),
    INDEX idx_payment_refund_provider_deleted (provider, deleted),
    INDEX idx_payment_refund_status_retry_deleted (status, next_retry_at, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_callback_log
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    payment_no        VARCHAR(64)     NOT NULL,
    provider          VARCHAR(32)     NULL,
    callback_no       VARCHAR(64)     NOT NULL,
    callback_status   VARCHAR(32)     NOT NULL,
    provider_event_type VARCHAR(64)   NULL,
    provider_txn_no   VARCHAR(128)    NULL,
    verified_app_id   VARCHAR(64)     NULL,
    verified_seller_id VARCHAR(64)    NULL,
    payload           TEXT            NULL,
    raw_payload_hash  VARCHAR(128)    NULL,
    idempotency_key   VARCHAR(128)    NOT NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_callback_no (callback_no),
    UNIQUE KEY uk_payment_callback_idem (idempotency_key),
    INDEX idx_payment_callback_payment_deleted (payment_no, deleted),
    INDEX idx_payment_callback_provider_deleted (provider, deleted),
    INDEX idx_payment_callback_provider_txn_deleted (provider_txn_no, deleted),
    INDEX idx_payment_callback_verified_app_deleted (verified_app_id, deleted)
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
    INDEX idx_outbox_status_next_retry_deleted (status, next_retry_at, deleted),
    INDEX idx_outbox_deleted_status_retry (deleted, status, next_retry_at, created_at)
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

CREATE TABLE IF NOT EXISTS dead_letter
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    topic       VARCHAR(128)    NOT NULL,
    msg_id      VARCHAR(64)     NOT NULL,
    payload     TEXT            NOT NULL,
    fail_reason VARCHAR(32)     NOT NULL,
    error_msg   VARCHAR(512)    NULL,
    status      TINYINT         NOT NULL DEFAULT 0,
    service     VARCHAR(64)     NOT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at  DATETIME        NULL,
    UNIQUE KEY uk_dead_letter_msg (topic, msg_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
