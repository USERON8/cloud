-- Contract-level schema sync for entity/dto/vo/feign alignment.
-- Target: MySQL 8.x

-- ==================== user_db ====================
ALTER TABLE user_db.admin
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER deleted;

ALTER TABLE user_db.merchant
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER deleted;

ALTER TABLE user_db.merchant_auth
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER deleted;

ALTER TABLE user_db.user_address
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER deleted;

-- ==================== order_db ====================
ALTER TABLE order_db.order_item
    ADD COLUMN create_by BIGINT UNSIGNED NULL AFTER price,
    ADD COLUMN update_by BIGINT UNSIGNED NULL AFTER create_by,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER deleted;

CREATE TABLE IF NOT EXISTS order_db.refunds
(
    id                    BIGINT UNSIGNED PRIMARY KEY,
    refund_no             VARCHAR(64)     NOT NULL UNIQUE,
    order_id              BIGINT UNSIGNED NOT NULL,
    order_no              VARCHAR(64)     NOT NULL,
    user_id               BIGINT UNSIGNED NOT NULL,
    merchant_id           BIGINT UNSIGNED NULL,
    refund_type           TINYINT         NOT NULL DEFAULT 1,
    refund_reason         VARCHAR(255)    NOT NULL,
    refund_description    TEXT            NULL,
    refund_amount         DECIMAL(10, 2)  NOT NULL,
    refund_quantity       INT             NOT NULL DEFAULT 1,
    status                TINYINT         NOT NULL DEFAULT 0,
    audit_time            DATETIME        NULL,
    audit_remark          VARCHAR(500)    NULL,
    logistics_company     VARCHAR(100)    NULL,
    logistics_no          VARCHAR(100)    NULL,
    refund_time           DATETIME        NULL,
    refund_channel        VARCHAR(50)     NULL,
    refund_transaction_no VARCHAR(100)    NULL,
    created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted            TINYINT         NOT NULL DEFAULT 0,
    INDEX idx_refund_order_id (order_id),
    INDEX idx_refund_user_id (user_id),
    INDEX idx_refund_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ==================== payment_db ====================
ALTER TABLE payment_db.payment_flow
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER deleted;

-- ==================== stock_db ====================
CREATE TABLE IF NOT EXISTS stock_db.stock_log
(
    id              BIGINT UNSIGNED PRIMARY KEY,
    product_id      BIGINT UNSIGNED NOT NULL,
    product_name    VARCHAR(200)    NULL,
    operation_type  VARCHAR(20)     NOT NULL,
    quantity_before INT             NOT NULL,
    quantity_after  INT             NOT NULL,
    quantity_change INT             NOT NULL,
    order_id        BIGINT UNSIGNED NULL,
    order_no        VARCHAR(64)     NULL,
    operator_id     BIGINT UNSIGNED NULL,
    operator_name   VARCHAR(100)    NULL,
    remark          VARCHAR(500)    NULL,
    operate_time    DATETIME        NULL,
    ip_address      VARCHAR(50)     NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    version         INT             NOT NULL DEFAULT 0,
    INDEX idx_stock_log_product_time (product_id, operate_time),
    INDEX idx_stock_log_order_id (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_db.stock_count
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    count_no          VARCHAR(50)     NOT NULL UNIQUE,
    product_id        BIGINT UNSIGNED NOT NULL,
    product_name      VARCHAR(200)    NULL,
    expected_quantity INT             NOT NULL,
    actual_quantity   INT             NOT NULL,
    difference        INT             NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    operator_id       BIGINT UNSIGNED NULL,
    operator_name     VARCHAR(100)    NULL,
    confirm_user_id   BIGINT UNSIGNED NULL,
    confirm_user_name VARCHAR(100)    NULL,
    count_time        DATETIME        NULL,
    confirm_time      DATETIME        NULL,
    remark            VARCHAR(500)    NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    INDEX idx_stock_count_product_time (product_id, count_time),
    INDEX idx_stock_count_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ==================== product_db ====================
ALTER TABLE product_db.category
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER level,
    ADD COLUMN create_by BIGINT UNSIGNED NULL AFTER status,
    ADD COLUMN update_by BIGINT UNSIGNED NULL AFTER create_by,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER deleted;

ALTER TABLE product_db.merchant_shop
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER deleted;

CREATE TABLE IF NOT EXISTS product_db.brand
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

CREATE TABLE IF NOT EXISTS product_db.brand_authorization
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

CREATE TABLE IF NOT EXISTS product_db.attribute_template
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

CREATE TABLE IF NOT EXISTS product_db.product_audit
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

CREATE TABLE IF NOT EXISTS product_db.product_review
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

CREATE TABLE IF NOT EXISTS product_db.product_sku
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

CREATE TABLE IF NOT EXISTS product_db.sku_specification
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

CREATE TABLE IF NOT EXISTS product_db.product_attribute
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
