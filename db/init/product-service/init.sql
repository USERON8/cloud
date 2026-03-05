DROP DATABASE IF EXISTS product_db;
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE product_db;

CREATE TABLE IF NOT EXISTS category
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    parent_id        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    name             VARCHAR(100)    NOT NULL,
    level            INT             NOT NULL,
    sort_order       INT             NOT NULL DEFAULT 0,
    status           INT             NOT NULL DEFAULT 1,
    create_by        BIGINT UNSIGNED NULL,
    update_by        BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_category_parent_level_deleted (parent_id, level, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_review
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    product_id       BIGINT UNSIGNED NOT NULL,
    product_name     VARCHAR(200)    NULL,
    sku_id           BIGINT UNSIGNED NOT NULL,
    order_id         BIGINT UNSIGNED NOT NULL,
    order_no         VARCHAR(64)     NOT NULL,
    user_id          BIGINT UNSIGNED NOT NULL,
    user_nickname    VARCHAR(100)    NULL,
    user_avatar      VARCHAR(500)    NULL,
    rating           TINYINT         NOT NULL,
    content          TEXT            NULL,
    images           JSON            NULL,
    tags             VARCHAR(500)    NULL,
    is_anonymous     TINYINT         NOT NULL DEFAULT 0,
    audit_status     VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    audit_time       DATETIME        NULL,
    audit_comment    VARCHAR(1000)   NULL,
    merchant_reply   TEXT            NULL,
    reply_time       DATETIME        NULL,
    like_count       INT             NOT NULL DEFAULT 0,
    is_visible       TINYINT         NOT NULL DEFAULT 1,
    review_type      VARCHAR(32)     NOT NULL DEFAULT 'NORMAL',
    parent_review_id BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_product_review_order_user (order_id, user_id),
    INDEX idx_product_review_product_status_deleted (product_id, audit_status, deleted),
    INDEX idx_product_review_sku_deleted (sku_id, deleted),
    INDEX idx_product_review_user_deleted (user_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    product_name     VARCHAR(200)    NOT NULL,
    price            DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    category_id      BIGINT UNSIGNED NULL,
    brand_id         BIGINT UNSIGNED NULL,
    status           INT             NOT NULL DEFAULT 1,
    stock_quantity   INT             NOT NULL DEFAULT 0,
    shop_id          BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_products_category_deleted (category_id, deleted),
    INDEX idx_products_brand_deleted (brand_id, deleted),
    INDEX idx_products_shop_deleted (shop_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_sku
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    product_id       BIGINT UNSIGNED NOT NULL,
    sku_code         VARCHAR(100)    NOT NULL,
    sku_name         VARCHAR(200)    NOT NULL,
    spec_values      VARCHAR(1000)   NULL,
    price            DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    original_price   DECIMAL(12, 2)  NULL,
    cost_price       DECIMAL(12, 2)  NULL,
    stock_quantity   INT             NOT NULL DEFAULT 0,
    sales_quantity   INT             NOT NULL DEFAULT 0,
    image_url        VARCHAR(500)    NULL,
    weight           INT             NULL,
    volume           INT             NULL,
    barcode          VARCHAR(100)    NULL,
    status           INT             NOT NULL DEFAULT 1,
    sort_order       INT             NOT NULL DEFAULT 0,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_product_sku_code (sku_code),
    INDEX idx_product_sku_product_deleted (product_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_attribute
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    product_id       BIGINT UNSIGNED NOT NULL,
    attr_name        VARCHAR(100)    NOT NULL,
    attr_value       VARCHAR(1000)   NULL,
    attr_group       VARCHAR(100)    NULL,
    attr_type        INT             NOT NULL DEFAULT 1,
    is_filterable    TINYINT         NOT NULL DEFAULT 0,
    is_list_visible  TINYINT         NOT NULL DEFAULT 1,
    is_detail_visible TINYINT        NOT NULL DEFAULT 1,
    sort_order       INT             NOT NULL DEFAULT 0,
    unit             VARCHAR(32)     NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_product_attribute_product_deleted (product_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_audit
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    product_id       BIGINT UNSIGNED NOT NULL,
    product_name     VARCHAR(200)    NOT NULL,
    merchant_id      BIGINT UNSIGNED NULL,
    merchant_name    VARCHAR(100)    NULL,
    audit_status     VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    audit_type       VARCHAR(32)     NOT NULL DEFAULT 'CREATE',
    submit_time      DATETIME        NULL,
    auditor_id       BIGINT UNSIGNED NULL,
    auditor_name     VARCHAR(100)    NULL,
    audit_time       DATETIME        NULL,
    audit_comment    VARCHAR(1000)   NULL,
    reject_reason    VARCHAR(1000)   NULL,
    product_snapshot TEXT            NULL,
    priority         INT             NOT NULL DEFAULT 0,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_product_audit_product_deleted (product_id, deleted),
    INDEX idx_product_audit_status_deleted (audit_status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sku_specification
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    spec_name        VARCHAR(100)    NOT NULL,
    spec_values      VARCHAR(1000)   NULL,
    category_id      BIGINT UNSIGNED NULL,
    spec_type        INT             NOT NULL DEFAULT 1,
    is_required      TINYINT         NOT NULL DEFAULT 0,
    sort_order       INT             NOT NULL DEFAULT 0,
    status           INT             NOT NULL DEFAULT 1,
    description      VARCHAR(1000)   NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_sku_spec_category_deleted (category_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS brand
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    brand_name       VARCHAR(100)    NOT NULL,
    brand_name_en    VARCHAR(100)    NULL,
    logo_url         VARCHAR(500)    NULL,
    description      VARCHAR(1000)   NULL,
    brand_story      TEXT            NULL,
    official_website VARCHAR(500)    NULL,
    country          VARCHAR(100)    NULL,
    founded_year     INT             NULL,
    status           INT             NOT NULL DEFAULT 1,
    is_hot           TINYINT         NOT NULL DEFAULT 0,
    is_recommended   TINYINT         NOT NULL DEFAULT 0,
    product_count    INT             NOT NULL DEFAULT 0,
    sort_order       INT             NOT NULL DEFAULT 0,
    seo_keywords     VARCHAR(500)    NULL,
    seo_description  VARCHAR(1000)   NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_brand_status_deleted (status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS brand_authorization
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    brand_id         BIGINT UNSIGNED NOT NULL,
    brand_name       VARCHAR(100)    NOT NULL,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    merchant_name    VARCHAR(100)    NULL,
    auth_type        VARCHAR(32)     NOT NULL DEFAULT 'NORMAL',
    auth_status      VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    certificate_url  VARCHAR(500)    NULL,
    start_time       DATETIME        NULL,
    end_time         DATETIME        NULL,
    auditor_id       BIGINT UNSIGNED NULL,
    auditor_name     VARCHAR(100)    NULL,
    audit_time       DATETIME        NULL,
    audit_comment    VARCHAR(1000)   NULL,
    remark           VARCHAR(1000)   NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_brand_auth_brand_deleted (brand_id, deleted),
    INDEX idx_brand_auth_merchant_deleted (merchant_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attribute_template
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    template_name    VARCHAR(100)    NOT NULL,
    category_id      BIGINT UNSIGNED NOT NULL,
    attributes       TEXT            NULL,
    description      VARCHAR(1000)   NULL,
    status           INT             NOT NULL DEFAULT 1,
    is_system        TINYINT         NOT NULL DEFAULT 0,
    usage_count      INT             NOT NULL DEFAULT 0,
    creator_id       BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_attribute_template_category_deleted (category_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_shop
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    shop_name        VARCHAR(100)    NOT NULL,
    avatar_url       VARCHAR(500)    NULL,
    description      VARCHAR(1000)   NULL,
    contact_phone    VARCHAR(32)     NULL,
    address          VARCHAR(255)    NULL,
    status           INT             NOT NULL DEFAULT 1,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_merchant_shop_merchant_deleted (merchant_id, deleted)
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
