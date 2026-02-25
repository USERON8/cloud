DROP DATABASE IF EXISTS product_db;
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE product_db;

CREATE TABLE IF NOT EXISTS category
(
    id         BIGINT UNSIGNED PRIMARY KEY,
    parent_id  BIGINT UNSIGNED NOT NULL DEFAULT 0,
    name       VARCHAR(100)    NOT NULL,
    level      TINYINT         NOT NULL,
    sort_order INT             NOT NULL DEFAULT 0,
    status     TINYINT         NOT NULL DEFAULT 1,
    create_by  BIGINT UNSIGNED NULL,
    update_by  BIGINT UNSIGNED NULL,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    version    INT             NOT NULL DEFAULT 0,
    INDEX idx_category_parent_status (parent_id, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_shop
(
    id            BIGINT UNSIGNED PRIMARY KEY,
    merchant_id   BIGINT UNSIGNED NOT NULL,
    shop_name     VARCHAR(200)    NOT NULL,
    avatar_url    VARCHAR(500)    NULL,
    description   TEXT            NULL,
    contact_phone VARCHAR(20)     NOT NULL,
    address       VARCHAR(500)    NOT NULL,
    status        TINYINT         NOT NULL DEFAULT 1,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    version       INT             NOT NULL DEFAULT 0,
    INDEX idx_merchant_shop_merchant (merchant_id),
    INDEX idx_merchant_shop_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products
(
    id             BIGINT UNSIGNED PRIMARY KEY,
    shop_id        BIGINT UNSIGNED NOT NULL,
    product_name   VARCHAR(100)    NOT NULL,
    price          DECIMAL(10, 2)  NOT NULL,
    stock_quantity INT             NOT NULL DEFAULT 0,
    category_id    BIGINT UNSIGNED NULL,
    brand_id       BIGINT UNSIGNED NULL,
    status         TINYINT         NOT NULL DEFAULT 0,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    version        INT             NOT NULL DEFAULT 0,
    INDEX idx_products_shop_status (shop_id, status),
    INDEX idx_products_category_status (category_id, status),
    INDEX idx_products_brand_status (brand_id, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS brand
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    brand_name       VARCHAR(200)  NOT NULL,
    brand_name_en    VARCHAR(200)  NULL,
    logo_url         VARCHAR(500)  NULL,
    description      VARCHAR(1000) NULL,
    brand_story      TEXT          NULL,
    official_website VARCHAR(500)  NULL,
    country          VARCHAR(100)  NULL,
    founded_year     INT           NULL,
    status           TINYINT       NOT NULL DEFAULT 1,
    is_hot           TINYINT       NOT NULL DEFAULT 0,
    is_recommended   TINYINT       NOT NULL DEFAULT 0,
    product_count    INT           NOT NULL DEFAULT 0,
    sort_order       INT           NULL DEFAULT 0,
    seo_keywords     VARCHAR(500)  NULL,
    seo_description  VARCHAR(1000) NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT       NOT NULL DEFAULT 0,
    version          INT           NOT NULL DEFAULT 0,
    INDEX idx_brand_name (brand_name),
    INDEX idx_brand_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS brand_authorization
(
    id              BIGINT UNSIGNED PRIMARY KEY,
    brand_id        BIGINT UNSIGNED NOT NULL,
    brand_name      VARCHAR(200)    NULL,
    merchant_id     BIGINT UNSIGNED NOT NULL,
    merchant_name   VARCHAR(200)    NULL,
    auth_type       VARCHAR(50)     NULL,
    auth_status     VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    certificate_url VARCHAR(500)    NULL,
    start_time      DATETIME        NULL,
    end_time        DATETIME        NULL,
    auditor_id      BIGINT UNSIGNED NULL,
    auditor_name    VARCHAR(100)    NULL,
    audit_time      DATETIME        NULL,
    audit_comment   VARCHAR(1000)   NULL,
    remark          VARCHAR(500)    NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    version         INT             NOT NULL DEFAULT 0,
    INDEX idx_brand_auth_brand (brand_id),
    INDEX idx_brand_auth_merchant (merchant_id),
    INDEX idx_brand_auth_status (auth_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attribute_template
(
    id            BIGINT UNSIGNED PRIMARY KEY,
    template_name VARCHAR(200)    NOT NULL,
    category_id   BIGINT UNSIGNED NOT NULL,
    attributes    TEXT            NULL,
    description   VARCHAR(500)    NULL,
    status        TINYINT         NOT NULL DEFAULT 1,
    is_system     TINYINT         NOT NULL DEFAULT 0,
    usage_count   INT             NOT NULL DEFAULT 0,
    creator_id    BIGINT UNSIGNED NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    version       INT             NOT NULL DEFAULT 0,
    INDEX idx_attr_tpl_category (category_id),
    INDEX idx_attr_tpl_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_audit
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    product_id       BIGINT UNSIGNED NOT NULL,
    product_name     VARCHAR(200)    NULL,
    merchant_id      BIGINT UNSIGNED NOT NULL,
    merchant_name    VARCHAR(200)    NULL,
    audit_status     VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    audit_type       VARCHAR(20)     NOT NULL,
    submit_time      DATETIME        NOT NULL,
    auditor_id       BIGINT UNSIGNED NULL,
    auditor_name     VARCHAR(100)    NULL,
    audit_time       DATETIME        NULL,
    audit_comment    VARCHAR(1000)   NULL,
    reject_reason    VARCHAR(1000)   NULL,
    product_snapshot TEXT            NULL,
    priority         TINYINT         NULL DEFAULT 2,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_product_audit_product (product_id),
    INDEX idx_product_audit_status (audit_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_review
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    product_id       BIGINT UNSIGNED NOT NULL,
    product_name     VARCHAR(200)    NULL,
    sku_id           BIGINT UNSIGNED NULL,
    order_id         BIGINT UNSIGNED NOT NULL,
    order_no         VARCHAR(64)     NULL,
    user_id          BIGINT UNSIGNED NOT NULL,
    user_nickname    VARCHAR(100)    NULL,
    user_avatar      VARCHAR(500)    NULL,
    rating           TINYINT         NOT NULL,
    content          TEXT            NULL,
    images           TEXT            NULL,
    tags             VARCHAR(500)    NULL,
    is_anonymous     TINYINT         NOT NULL DEFAULT 0,
    audit_status     VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    audit_time       DATETIME        NULL,
    audit_comment    VARCHAR(500)    NULL,
    merchant_reply   TEXT            NULL,
    reply_time       DATETIME        NULL,
    like_count       INT             NOT NULL DEFAULT 0,
    is_visible       TINYINT         NOT NULL DEFAULT 1,
    review_type      VARCHAR(20)     NOT NULL DEFAULT 'INITIAL',
    parent_review_id BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_product_review_product (product_id),
    INDEX idx_product_review_order (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_sku
(
    id             BIGINT UNSIGNED PRIMARY KEY,
    product_id     BIGINT UNSIGNED NOT NULL,
    sku_code       VARCHAR(100)    NOT NULL,
    sku_name       VARCHAR(200)    NOT NULL,
    spec_values    TEXT            NULL,
    price          DECIMAL(10, 2)  NOT NULL,
    original_price DECIMAL(10, 2)  NULL,
    cost_price     DECIMAL(10, 2)  NULL,
    stock_quantity INT             NOT NULL DEFAULT 0,
    sales_quantity INT             NOT NULL DEFAULT 0,
    image_url      VARCHAR(500)    NULL,
    weight         INT             NULL,
    volume         INT             NULL,
    barcode        VARCHAR(100)    NULL,
    status         TINYINT         NOT NULL DEFAULT 1,
    sort_order     INT             NULL DEFAULT 0,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    version        INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_product_sku_code (sku_code),
    INDEX idx_product_sku_product (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sku_specification
(
    id          BIGINT UNSIGNED PRIMARY KEY,
    spec_name   VARCHAR(100)    NOT NULL,
    spec_values TEXT            NULL,
    category_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    spec_type   TINYINT         NOT NULL DEFAULT 1,
    is_required TINYINT         NOT NULL DEFAULT 0,
    sort_order  INT             NULL DEFAULT 0,
    status      TINYINT         NOT NULL DEFAULT 1,
    description VARCHAR(500)    NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT         NOT NULL DEFAULT 0,
    version     INT             NOT NULL DEFAULT 0,
    INDEX idx_sku_spec_category (category_id),
    INDEX idx_sku_spec_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_attribute
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    product_id        BIGINT UNSIGNED NOT NULL,
    attr_name         VARCHAR(100)    NOT NULL,
    attr_value        VARCHAR(500)    NOT NULL,
    attr_group        VARCHAR(100)    NULL,
    attr_type         TINYINT         NOT NULL DEFAULT 1,
    is_filterable     TINYINT         NOT NULL DEFAULT 0,
    is_list_visible   TINYINT         NOT NULL DEFAULT 1,
    is_detail_visible TINYINT         NOT NULL DEFAULT 1,
    sort_order        INT             NULL DEFAULT 0,
    unit              VARCHAR(20)     NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    INDEX idx_product_attr_product (product_id),
    INDEX idx_product_attr_group (attr_group)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
