DROP DATABASE IF EXISTS order_db;
CREATE DATABASE IF NOT EXISTS order_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE order_db;

CREATE TABLE IF NOT EXISTS order_main
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    main_order_no    VARCHAR(64)     NOT NULL,
    user_id          BIGINT UNSIGNED NOT NULL,
    order_status     VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    total_amount     DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    payable_amount   DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    pay_channel      VARCHAR(32)     NULL,
    paid_at          DATETIME        NULL,
    cancelled_at     DATETIME        NULL,
    cancel_reason    VARCHAR(255)    NULL,
    remark           VARCHAR(255)    NULL,
    client_order_id  VARCHAR(64)     NOT NULL,
    idempotency_key  VARCHAR(128)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_main_no (main_order_no),
    UNIQUE KEY uk_order_main_user_client_order (user_id, client_order_id),
    UNIQUE KEY uk_order_main_idempotency_key (idempotency_key),
    INDEX idx_order_main_user_client_deleted (user_id, client_order_id, deleted),
    INDEX idx_order_main_idempotency_deleted (idempotency_key, deleted),
    INDEX idx_order_main_user_status_deleted (user_id, order_status, deleted),
    INDEX idx_order_main_created_deleted (created_at, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_sub
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    sub_order_no     VARCHAR(64)     NOT NULL,
    main_order_id    BIGINT UNSIGNED NOT NULL,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    order_status     VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    shipping_status  VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    after_sale_status VARCHAR(32)    NOT NULL DEFAULT 'NONE',
    item_amount      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    shipping_fee     DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    discount_amount  DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    payable_amount   DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    receiver_name    VARCHAR(64)     NULL,
    receiver_phone   VARCHAR(32)     NULL,
    receiver_address VARCHAR(255)    NULL,
    shipping_company VARCHAR(50)     NULL,
    tracking_number  VARCHAR(100)    NULL,
    shipped_at       DATETIME        NULL,
    estimated_arrival DATE           NULL,
    received_at      DATETIME        NULL,
    done_at          DATETIME        NULL,
    closed_at        DATETIME        NULL,
    close_reason     VARCHAR(255)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_sub_no (sub_order_no),
    INDEX idx_order_sub_main_deleted (main_order_id, deleted),
    INDEX idx_order_sub_merchant_status_deleted (merchant_id, order_status, deleted),
    INDEX idx_order_sub_after_sale_deleted (after_sale_status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_item
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    main_order_id    BIGINT UNSIGNED NOT NULL,
    sub_order_id     BIGINT UNSIGNED NOT NULL,
    spu_id           BIGINT UNSIGNED NOT NULL,
    sku_id           BIGINT UNSIGNED NOT NULL,
    sku_code         VARCHAR(64)     NOT NULL,
    sku_name         VARCHAR(255)    NOT NULL,
    sku_snapshot     JSON            NOT NULL,
    quantity         INT             NOT NULL,
    unit_price       DECIMAL(12, 2)  NOT NULL,
    total_price      DECIMAL(12, 2)  NOT NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_order_item_main_deleted (main_order_id, deleted),
    INDEX idx_order_item_sub_deleted (sub_order_id, deleted),
    INDEX idx_order_item_sku_deleted (sku_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_main_archive
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    main_order_no    VARCHAR(64)     NOT NULL,
    user_id          BIGINT UNSIGNED NOT NULL,
    order_status     VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    total_amount     DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    payable_amount   DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    pay_channel      VARCHAR(32)     NULL,
    paid_at          DATETIME        NULL,
    cancelled_at     DATETIME        NULL,
    cancel_reason    VARCHAR(255)    NULL,
    remark           VARCHAR(255)    NULL,
    client_order_id  VARCHAR(64)     NOT NULL,
    idempotency_key  VARCHAR(128)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    archived_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_main_archive_no (main_order_no),
    UNIQUE KEY uk_order_main_archive_user_client_order (user_id, client_order_id),
    UNIQUE KEY uk_order_main_archive_idempotency_key (idempotency_key),
    INDEX idx_order_main_archive_user_status_deleted (user_id, order_status, deleted),
    INDEX idx_order_main_archive_created_deleted (created_at, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_sub_archive
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    sub_order_no     VARCHAR(64)     NOT NULL,
    main_order_id    BIGINT UNSIGNED NOT NULL,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    order_status     VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    shipping_status  VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    after_sale_status VARCHAR(32)    NOT NULL DEFAULT 'NONE',
    item_amount      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    shipping_fee     DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    discount_amount  DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    payable_amount   DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    receiver_name    VARCHAR(64)     NULL,
    receiver_phone   VARCHAR(32)     NULL,
    receiver_address VARCHAR(255)    NULL,
    shipping_company VARCHAR(50)     NULL,
    tracking_number  VARCHAR(100)    NULL,
    shipped_at       DATETIME        NULL,
    estimated_arrival DATE           NULL,
    received_at      DATETIME        NULL,
    done_at          DATETIME        NULL,
    closed_at        DATETIME        NULL,
    close_reason     VARCHAR(255)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    archived_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_sub_archive_no (sub_order_no),
    INDEX idx_order_sub_archive_main_deleted (main_order_id, deleted),
    INDEX idx_order_sub_archive_merchant_status_deleted (merchant_id, order_status, deleted),
    INDEX idx_order_sub_archive_after_sale_deleted (after_sale_status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_item_archive
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    main_order_id    BIGINT UNSIGNED NOT NULL,
    sub_order_id     BIGINT UNSIGNED NOT NULL,
    spu_id           BIGINT UNSIGNED NOT NULL,
    sku_id           BIGINT UNSIGNED NOT NULL,
    sku_code         VARCHAR(64)     NOT NULL,
    sku_name         VARCHAR(255)    NOT NULL,
    sku_snapshot     JSON            NOT NULL,
    quantity         INT             NOT NULL,
    unit_price       DECIMAL(12, 2)  NOT NULL,
    total_price      DECIMAL(12, 2)  NOT NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    archived_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_item_archive_main_deleted (main_order_id, deleted),
    INDEX idx_order_item_archive_sub_deleted (sub_order_id, deleted),
    INDEX idx_order_item_archive_sku_deleted (sku_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cart
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    cart_no          VARCHAR(64)     NOT NULL,
    user_id          BIGINT UNSIGNED NOT NULL,
    cart_status      VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    selected_count   INT             NOT NULL DEFAULT 0,
    total_amount     DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_cart_no (cart_no),
    UNIQUE KEY uk_cart_user_status (user_id, cart_status),
    INDEX idx_cart_user_deleted (user_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cart_item
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    cart_id          BIGINT UNSIGNED NOT NULL,
    user_id          BIGINT UNSIGNED NOT NULL,
    spu_id           BIGINT UNSIGNED NOT NULL,
    sku_id           BIGINT UNSIGNED NOT NULL,
    sku_name         VARCHAR(255)    NOT NULL,
    quantity         INT             NOT NULL,
    unit_price       DECIMAL(12, 2)  NOT NULL,
    selected         TINYINT         NOT NULL DEFAULT 1,
    checked_out      TINYINT         NOT NULL DEFAULT 0,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_cart_item_user_sku (user_id, sku_id),
    INDEX idx_cart_item_cart_deleted (cart_id, deleted),
    INDEX idx_cart_item_user_selected_deleted (user_id, selected, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS after_sale
(
    id                 BIGINT UNSIGNED PRIMARY KEY,
    after_sale_no      VARCHAR(64)     NOT NULL,
    main_order_id      BIGINT UNSIGNED NOT NULL,
    sub_order_id       BIGINT UNSIGNED NOT NULL,
    user_id            BIGINT UNSIGNED NOT NULL,
    merchant_id        BIGINT UNSIGNED NOT NULL,
    after_sale_type    VARCHAR(16)     NOT NULL COMMENT 'REFUND|RETURN_REFUND',
    status             VARCHAR(32)     NOT NULL DEFAULT 'APPLIED',
    reason             VARCHAR(255)    NOT NULL,
    description        VARCHAR(1000)   NULL,
    apply_amount       DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    approved_amount    DECIMAL(12, 2)  NULL,
    return_logistics_company VARCHAR(64) NULL,
    return_logistics_no VARCHAR(64)    NULL,
    refund_channel     VARCHAR(32)     NULL,
    refunded_at        DATETIME        NULL,
    closed_at          DATETIME        NULL,
    close_reason       VARCHAR(255)    NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT         NOT NULL DEFAULT 0,
    version            INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_after_sale_no (after_sale_no),
    INDEX idx_after_sale_sub_status_deleted (sub_order_id, status, deleted),
    INDEX idx_after_sale_user_status_deleted (user_id, status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS after_sale_item
(
    id                 BIGINT UNSIGNED PRIMARY KEY,
    after_sale_id      BIGINT UNSIGNED NOT NULL,
    order_item_id      BIGINT UNSIGNED NOT NULL,
    sku_id             BIGINT UNSIGNED NOT NULL,
    quantity           INT             NOT NULL,
    apply_amount       DECIMAL(12, 2)  NOT NULL,
    approved_amount    DECIMAL(12, 2)  NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT         NOT NULL DEFAULT 0,
    version            INT             NOT NULL DEFAULT 0,
    INDEX idx_after_sale_item_after_deleted (after_sale_id, deleted),
    INDEX idx_after_sale_item_order_item_deleted (order_item_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS after_sale_evidence
(
    id                 BIGINT UNSIGNED PRIMARY KEY,
    after_sale_id      BIGINT UNSIGNED NOT NULL,
    evidence_type      VARCHAR(32)     NOT NULL COMMENT 'IMAGE|VIDEO|DOC',
    object_key         VARCHAR(255)    NOT NULL,
    object_url         VARCHAR(500)    NOT NULL,
    uploaded_by        BIGINT UNSIGNED NOT NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT         NOT NULL DEFAULT 0,
    version            INT             NOT NULL DEFAULT 0,
    INDEX idx_after_sale_evidence_after_deleted (after_sale_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS after_sale_timeline
(
    id                 BIGINT UNSIGNED PRIMARY KEY,
    after_sale_id      BIGINT UNSIGNED NOT NULL,
    from_status        VARCHAR(32)     NULL,
    to_status          VARCHAR(32)     NOT NULL,
    action             VARCHAR(64)     NOT NULL,
    operator_id        BIGINT UNSIGNED NULL,
    operator_role      VARCHAR(32)     NULL,
    remark             VARCHAR(500)    NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT         NOT NULL DEFAULT 0,
    version            INT             NOT NULL DEFAULT 0,
    INDEX idx_after_sale_timeline_after_deleted (after_sale_id, deleted),
    INDEX idx_after_sale_timeline_created_deleted (created_at, deleted)
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
    INDEX idx_outbox_status_next_retry_deleted (status, next_retry_at, deleted),
    INDEX idx_outbox_deleted_status_retry (deleted, status, next_retry_at, created_at)
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
