DROP DATABASE IF EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS payment_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE payment_db;

CREATE TABLE IF NOT EXISTS payment
(
    id             BIGINT UNSIGNED PRIMARY KEY,
    order_id       BIGINT UNSIGNED NOT NULL,
    user_id        BIGINT UNSIGNED NOT NULL,
    amount         DECIMAL(10, 2)  NOT NULL,
    status         TINYINT         NOT NULL,
    channel        TINYINT         NOT NULL,
    transaction_id VARCHAR(100)    NULL,
    trace_id       VARCHAR(64)     NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    version        INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_order_id (order_id),
    INDEX idx_payment_user_status_deleted (user_id, status, deleted),
    INDEX idx_payment_trace_id_deleted (trace_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_flow
(
    id         BIGINT UNSIGNED PRIMARY KEY,
    payment_id BIGINT UNSIGNED NOT NULL,
    flow_type  TINYINT         NOT NULL,
    amount     DECIMAL(10, 2)  NOT NULL,
    trace_id   VARCHAR(64)     NULL,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    version    INT             NOT NULL DEFAULT 0,
    INDEX idx_payment_flow_payment_id_deleted (payment_id, deleted),
    INDEX idx_payment_flow_trace_id_deleted (trace_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
