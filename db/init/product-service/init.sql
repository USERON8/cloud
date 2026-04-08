DROP DATABASE IF EXISTS product_db;
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE product_db;

CREATE TABLE IF NOT EXISTS category
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    parent_id        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    name             VARCHAR(100)    NOT NULL,
    level            TINYINT         NOT NULL,
    path             VARCHAR(255)    NOT NULL,
    sort_order       INT             NOT NULL DEFAULT 0,
    status           TINYINT         NOT NULL DEFAULT 1,
    create_by        BIGINT UNSIGNED NULL,
    update_by        BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    CONSTRAINT chk_category_level CHECK (level IN (1, 2, 3)),
    INDEX idx_category_parent_deleted (parent_id, deleted),
    INDEX idx_category_level_deleted (level, deleted),
    INDEX idx_category_path_deleted (path, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spu
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    spu_name         VARCHAR(200)    NOT NULL,
    subtitle         VARCHAR(255)    NULL,
    category_id      BIGINT UNSIGNED NOT NULL,
    brand_id         BIGINT UNSIGNED NULL,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    status           TINYINT         NOT NULL DEFAULT 1,
    description      TEXT            NULL,
    main_image       VARCHAR(500)    NULL,
    main_image_file  VARCHAR(500)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_spu_category_status_deleted (category_id, status, deleted),
    INDEX idx_spu_merchant_deleted (merchant_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sku
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    spu_id           BIGINT UNSIGNED NOT NULL,
    sku_code         VARCHAR(100)    NOT NULL,
    sku_name         VARCHAR(200)    NOT NULL,
    spec_json        JSON            NULL,
    sale_price       DECIMAL(12, 2)  NOT NULL,
    market_price     DECIMAL(12, 2)  NULL,
    cost_price       DECIMAL(12, 2)  NULL,
    status           TINYINT         NOT NULL DEFAULT 1,
    image_url        VARCHAR(500)    NULL,
    image_file       VARCHAR(500)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sku_code (sku_code),
    INDEX idx_sku_spu_status_deleted (spu_id, status, deleted),
    INDEX idx_sku_spu_deleted (spu_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_review
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    spu_id           BIGINT UNSIGNED NOT NULL,
    sku_id           BIGINT UNSIGNED NOT NULL,
    order_sub_no     VARCHAR(64)     NOT NULL,
    user_id          BIGINT UNSIGNED NOT NULL,
    rating           TINYINT         NOT NULL,
    content          TEXT            NULL,
    images           JSON            NULL,
    tags             VARCHAR(500)    NULL,
    is_anonymous     TINYINT         NOT NULL DEFAULT 0,
    audit_status     VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    merchant_reply   TEXT            NULL,
    reply_time       DATETIME        NULL,
    like_count       INT             NOT NULL DEFAULT 0,
    is_visible       TINYINT         NOT NULL DEFAULT 1,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_review_order_user (order_sub_no, user_id),
    INDEX idx_review_spu_status_deleted (spu_id, audit_status, deleted),
    INDEX idx_review_sku_deleted (sku_id, deleted),
    INDEX idx_review_user_deleted (user_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_shop
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    shop_name        VARCHAR(100)    NOT NULL,
    avatar_url       VARCHAR(500)    NULL,
    description      VARCHAR(500)    NULL,
    contact_phone    VARCHAR(20)     NULL,
    address          VARCHAR(200)    NULL,
    status           TINYINT         NOT NULL DEFAULT 1,
    create_by        BIGINT UNSIGNED NULL,
    update_by        BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_shop_merchant_deleted (merchant_id, deleted),
    INDEX idx_shop_status_deleted (status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS brand
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    brand_name       VARCHAR(100)    NOT NULL,
    brand_name_en    VARCHAR(100)    NULL,
    logo_url         VARCHAR(500)    NULL,
    description      TEXT            NULL,
    brand_story      TEXT            NULL,
    official_website VARCHAR(255)    NULL,
    country          VARCHAR(50)     NULL,
    founded_year     INT             NULL,
    status           TINYINT         NOT NULL DEFAULT 1,
    is_hot           TINYINT         NOT NULL DEFAULT 0,
    is_recommended   TINYINT         NOT NULL DEFAULT 0,
    product_count    INT             NOT NULL DEFAULT 0,
    sort_order       INT             NOT NULL DEFAULT 0,
    seo_keywords     VARCHAR(255)    NULL,
    seo_description  VARCHAR(500)    NULL,
    create_by        BIGINT UNSIGNED NULL,
    update_by        BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_brand_status_deleted (status, deleted),
    INDEX idx_brand_hot_deleted (is_hot, deleted),
    INDEX idx_brand_recommended_deleted (is_recommended, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS brand_authorization
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    brand_id         BIGINT UNSIGNED NOT NULL,
    brand_name       VARCHAR(100)    NOT NULL,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    merchant_name    VARCHAR(100)    NOT NULL,
    auth_type        VARCHAR(32)     NOT NULL,
    auth_status      VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    certificate_url  VARCHAR(500)    NULL,
    start_time       DATETIME        NULL,
    end_time         DATETIME        NULL,
    auditor_id       BIGINT UNSIGNED NULL,
    auditor_name     VARCHAR(100)    NULL,
    audit_time       DATETIME        NULL,
    audit_comment    VARCHAR(500)    NULL,
    remark           VARCHAR(500)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_auth_brand_deleted (brand_id, deleted),
    INDEX idx_auth_merchant_deleted (merchant_id, deleted),
    INDEX idx_auth_status_deleted (auth_status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attribute_template
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    template_name    VARCHAR(100)    NOT NULL,
    category_id      BIGINT UNSIGNED NOT NULL,
    attributes       JSON            NULL,
    description      VARCHAR(500)    NULL,
    status           TINYINT         NOT NULL DEFAULT 1,
    is_system        TINYINT         NOT NULL DEFAULT 0,
    usage_count      INT             NOT NULL DEFAULT 0,
    creator_id       BIGINT UNSIGNED NULL,
    create_by        BIGINT UNSIGNED NULL,
    update_by        BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_template_category_deleted (category_id, deleted),
    INDEX idx_template_status_deleted (status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_attribute
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    product_id       BIGINT UNSIGNED NOT NULL,
    attr_name        VARCHAR(100)    NOT NULL,
    attr_value       VARCHAR(255)    NOT NULL,
    attr_group       VARCHAR(100)    NULL,
    attr_type        TINYINT         NOT NULL DEFAULT 1,
    is_filterable    TINYINT         NOT NULL DEFAULT 0,
    is_list_visible  TINYINT         NOT NULL DEFAULT 1,
    is_detail_visible TINYINT        NOT NULL DEFAULT 1,
    sort_order       INT             NOT NULL DEFAULT 0,
    unit             VARCHAR(20)     NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_attr_product_deleted (product_id, deleted),
    INDEX idx_attr_group_deleted (attr_group, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_audit
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    product_id       BIGINT UNSIGNED NOT NULL,
    product_name     VARCHAR(200)    NOT NULL,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    merchant_name    VARCHAR(100)    NOT NULL,
    audit_status     VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    audit_type       VARCHAR(32)     NOT NULL,
    submit_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    auditor_id       BIGINT UNSIGNED NULL,
    auditor_name     VARCHAR(100)    NULL,
    audit_time       DATETIME        NULL,
    audit_comment    VARCHAR(500)    NULL,
    reject_reason    VARCHAR(500)    NULL,
    product_snapshot JSON            NULL,
    priority         INT             NOT NULL DEFAULT 0,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_audit_product_deleted (product_id, deleted),
    INDEX idx_audit_merchant_deleted (merchant_id, deleted),
    INDEX idx_audit_status_deleted (audit_status, deleted),
    INDEX idx_audit_priority_deleted (priority, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sku_specification
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    spec_name        VARCHAR(100)    NOT NULL,
    spec_values      JSON            NOT NULL,
    category_id      BIGINT UNSIGNED NOT NULL,
    spec_type        TINYINT         NOT NULL DEFAULT 1,
    is_required      TINYINT         NOT NULL DEFAULT 0,
    sort_order       INT             NOT NULL DEFAULT 0,
    status           TINYINT         NOT NULL DEFAULT 1,
    description      VARCHAR(500)    NULL,
    create_by        BIGINT UNSIGNED NULL,
    update_by        BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_spec_category_deleted (category_id, deleted),
    INDEX idx_spec_status_deleted (status, deleted)
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

