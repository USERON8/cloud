DROP DATABASE IF EXISTS user_db;
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_db;

CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT UNSIGNED PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    phone      VARCHAR(20)  NULL,
    nickname   VARCHAR(50)  NOT NULL,
    avatar_url VARCHAR(255) NULL,
    email      VARCHAR(100) NULL,
    status     TINYINT      NOT NULL DEFAULT 1,
    enabled    TINYINT      NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    version    INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_users_username_deleted (username, deleted),
    INDEX idx_users_deleted (deleted),
    UNIQUE KEY uk_users_phone (phone),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_status_deleted (status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles
(
    id          BIGINT UNSIGNED PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    version     INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_roles_code_deleted (code, deleted),
    INDEX idx_roles_deleted (deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permissions
(
    id         BIGINT UNSIGNED PRIMARY KEY,
    code       VARCHAR(100) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    module     VARCHAR(50)  NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    version    INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_permissions_code_deleted (code, deleted),
    INDEX idx_permissions_module_deleted (module, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles
(
    id         BIGINT UNSIGNED PRIMARY KEY,
    user_id    BIGINT UNSIGNED NOT NULL,
    role_id    BIGINT UNSIGNED NOT NULL,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    version    INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_roles_user_role_deleted (user_id, role_id, deleted),
    INDEX idx_user_roles_user_deleted (user_id, deleted),
    INDEX idx_user_roles_role_deleted (role_id, deleted),
    INDEX idx_deleted (deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permissions
(
    id            BIGINT UNSIGNED PRIMARY KEY,
    role_id       BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    version       INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_permissions_role_permission_deleted (role_id, permission_id, deleted),
    INDEX idx_role_permissions_role_deleted (role_id, deleted),
    INDEX idx_role_permissions_permission_deleted (permission_id, deleted),
    INDEX idx_deleted (deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO roles (id, code, name, description, deleted, version)
VALUES (26001, 'ROLE_USER', '普通用户', '基础用户角色', 0, 0),
       (26002, 'ROLE_MERCHANT', '商家', '商家角色', 0, 0),
       (26003, 'ROLE_ADMIN', '管理员', '管理员角色', 0, 0)
ON DUPLICATE KEY UPDATE code        = VALUES(code),
                        name        = VALUES(name),
                        description = VALUES(description),
                        deleted     = VALUES(deleted),
                        version     = VALUES(version);

INSERT INTO permissions (id, code, name, module, deleted, version)
VALUES (27001, 'order:create', '创建订单', 'order', 0, 0),
       (27002, 'order:cancel', '取消订单', 'order', 0, 0),
       (27003, 'order:query', '查询订单', 'order', 0, 0),
       (27004, 'order:refund', '申请退款', 'order', 0, 0),
       (27011, 'product:view', '查看商品', 'product', 0, 0),
       (27012, 'product:create', '创建商品', 'product', 0, 0),
       (27013, 'product:edit', '编辑商品', 'product', 0, 0),
       (27014, 'product:delete', '删除商品', 'product', 0, 0),
       (27021, 'user:profile', '编辑资料', 'user', 0, 0),
       (27022, 'user:address', '管理地址', 'user', 0, 0),
       (27031, 'merchant:manage', '商家管理', 'merchant', 0, 0),
       (27032, 'merchant:audit', '商家审核', 'merchant', 0, 0),
       (27041, 'admin:all', '所有权限', 'admin', 0, 0)
ON DUPLICATE KEY UPDATE code    = VALUES(code),
                        name    = VALUES(name),
                        module  = VALUES(module),
                        deleted = VALUES(deleted),
                        version = VALUES(version);

INSERT INTO role_permissions (id, role_id, permission_id, deleted, version)
VALUES (28001, 26001, 27001, 0, 0),
       (28002, 26001, 27002, 0, 0),
       (28003, 26001, 27003, 0, 0),
       (28004, 26001, 27004, 0, 0),
       (28005, 26001, 27011, 0, 0),
       (28006, 26001, 27021, 0, 0),
       (28007, 26001, 27022, 0, 0),
       (28011, 26002, 27011, 0, 0),
       (28012, 26002, 27012, 0, 0),
       (28013, 26002, 27013, 0, 0),
       (28014, 26002, 27014, 0, 0),
       (28015, 26002, 27031, 0, 0),
       (28016, 26002, 27003, 0, 0),
       (28031, 26003, 27041, 0, 0),
       (28032, 26003, 27032, 0, 0),
       (28033, 26003, 27031, 0, 0),
       (28034, 26003, 27011, 0, 0),
       (28035, 26003, 27012, 0, 0),
       (28036, 26003, 27013, 0, 0),
       (28037, 26003, 27014, 0, 0),
       (28038, 26003, 27001, 0, 0),
       (28039, 26003, 27002, 0, 0),
       (28040, 26003, 27003, 0, 0),
       (28041, 26003, 27004, 0, 0),
       (28042, 26003, 27021, 0, 0),
       (28043, 26003, 27022, 0, 0)
ON DUPLICATE KEY UPDATE role_id       = VALUES(role_id),
                        permission_id = VALUES(permission_id),
                        deleted       = VALUES(deleted),
                        version       = VALUES(version);

CREATE TABLE IF NOT EXISTS user_profile_ext
(
    id            BIGINT UNSIGNED PRIMARY KEY,
    user_id       BIGINT UNSIGNED NOT NULL,
    gender        VARCHAR(16)     NULL,
    birthday      DATE            NULL,
    bio           VARCHAR(500)    NULL,
    country       VARCHAR(64)     NULL,
    province      VARCHAR(64)     NULL,
    city          VARCHAR(64)     NULL,
    personal_tags JSON            NULL,
    preferences   JSON            NULL,
    last_login_at DATETIME        NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    version       INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_profile_ext_user_id (user_id),
    INDEX idx_user_profile_ext_city_deleted (city, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_address
(
    id             BIGINT UNSIGNED PRIMARY KEY,
    user_id        BIGINT UNSIGNED NOT NULL,
    address_tag    VARCHAR(32)     NULL,
    receiver_name  VARCHAR(50)     NOT NULL,
    receiver_phone VARCHAR(20)     NOT NULL,
    country        VARCHAR(64)     NOT NULL DEFAULT 'China',
    province       VARCHAR(64)     NOT NULL,
    city           VARCHAR(64)     NOT NULL,
    district       VARCHAR(64)     NOT NULL,
    street         VARCHAR(100)    NOT NULL,
    detail_address VARCHAR(255)    NOT NULL,
    postal_code    VARCHAR(16)     NULL,
    longitude      DECIMAL(11, 7)  NULL,
    latitude       DECIMAL(11, 7)  NULL,
    is_default     TINYINT         NOT NULL DEFAULT 0,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    version        INT             NOT NULL DEFAULT 0,
    INDEX idx_user_address_user_deleted (user_id, deleted),
    INDEX idx_user_address_user_default_deleted (user_id, is_default, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_favorite
(
    id              BIGINT UNSIGNED PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    spu_id          BIGINT UNSIGNED NOT NULL,
    sku_id          BIGINT UNSIGNED NULL,
    favorite_status VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    version         INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_favorite_user_spu_sku (user_id, spu_id, sku_id),
    INDEX idx_user_favorite_user_status_deleted (user_id, favorite_status, deleted),
    INDEX idx_user_favorite_spu_deleted (spu_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin
(
    id         BIGINT UNSIGNED PRIMARY KEY,
    username   VARCHAR(50) NOT NULL,
    real_name  VARCHAR(50) NOT NULL,
    phone      VARCHAR(20) NULL,
    role       VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    status     TINYINT     NOT NULL DEFAULT 1,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT     NOT NULL DEFAULT 0,
    version    INT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_admin_username_deleted (username, deleted),
    INDEX idx_admin_deleted (deleted),
    INDEX idx_admin_status_deleted (status, deleted),
    INDEX idx_admin_role_deleted (role, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant
(
    id            BIGINT UNSIGNED PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    phone         VARCHAR(20)  NULL,
    email         VARCHAR(128) NULL COMMENT '商户邮箱',
    status        TINYINT      NOT NULL DEFAULT 1,
    audit_status  TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    version       INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_merchant_username_deleted (username, deleted),
    INDEX idx_merchant_deleted (deleted),
    INDEX idx_merchant_status_deleted (status, deleted),
    INDEX idx_merchant_audit_status_deleted (audit_status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_auth
(
    id                      BIGINT UNSIGNED PRIMARY KEY,
    merchant_id             BIGINT UNSIGNED NOT NULL,
    business_license_number VARCHAR(50)     NOT NULL,
    business_license_url    VARCHAR(255)    NOT NULL,
    id_card_front_url       VARCHAR(255)    NOT NULL,
    id_card_back_url        VARCHAR(255)    NOT NULL,
    contact_phone           VARCHAR(20)     NOT NULL,
    contact_address         VARCHAR(255)    NOT NULL,
    auth_status             TINYINT         NOT NULL DEFAULT 0,
    auth_remark             VARCHAR(255)    NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                 TINYINT         NOT NULL DEFAULT 0,
    version                 INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_merchant_auth_merchant_id (merchant_id),
    INDEX idx_merchant_auth_status_deleted (auth_status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS operation_audit_log
(
    id              BIGINT UNSIGNED PRIMARY KEY,
    operator_id     BIGINT UNSIGNED NOT NULL,
    operator_role   VARCHAR(32)     NOT NULL,
    action          VARCHAR(128)    NOT NULL,
    target_type     VARCHAR(64)     NOT NULL,
    target_id       VARCHAR(64)     NOT NULL,
    trace_id        VARCHAR(64)     NULL,
    request_uri     VARCHAR(255)    NULL,
    request_method  VARCHAR(16)     NULL,
    request_payload TEXT            NULL,
    result_code     INT             NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    version         INT             NOT NULL DEFAULT 0,
    INDEX idx_operation_audit_operator_created_deleted (operator_id, created_at, deleted),
    INDEX idx_operation_audit_target_deleted (target_type, target_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outbox_event
(
    id             BIGINT UNSIGNED PRIMARY KEY,
    event_id       VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   VARCHAR(64) NOT NULL,
    event_type     VARCHAR(64) NOT NULL,
    payload        JSON        NOT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'NEW',
    retry_count    INT         NOT NULL DEFAULT 0,
    next_retry_at  DATETIME    NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT     NOT NULL DEFAULT 0,
    version        INT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_outbox_event_id (event_id),
    INDEX idx_outbox_status_next_retry_deleted (status, next_retry_at, deleted),
    INDEX idx_outbox_deleted_status_retry (deleted, status, next_retry_at, created_at)
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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS inbox_consume_log
(
    id             BIGINT UNSIGNED PRIMARY KEY,
    event_id       VARCHAR(64)   NOT NULL,
    consumer_group VARCHAR(64)   NOT NULL,
    event_type     VARCHAR(64)   NOT NULL,
    consume_status VARCHAR(16)   NOT NULL DEFAULT 'SUCCESS',
    error_message  VARCHAR(1000) NULL,
    consumed_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT       NOT NULL DEFAULT 0,
    version        INT           NOT NULL DEFAULT 0,
    UNIQUE KEY uk_inbox_event_group (event_id, consumer_group),
    INDEX idx_inbox_event_type_deleted (event_type, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS test_access_token
(
    id          BIGINT UNSIGNED PRIMARY KEY,
    token_value VARCHAR(255) NOT NULL,
    token_owner VARCHAR(64)  NOT NULL DEFAULT 'test-user',
    expires_at  DATETIME     NOT NULL,
    is_active   TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_test_access_token_value (token_value),
    INDEX idx_test_access_token_active_expires (is_active, expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO test_access_token (id, token_value, token_owner, expires_at, is_active)
VALUES (1, 'TEST_ENV_PERMANENT_TOKEN', 'test-user', '2099-12-31 23:59:59', 1)
ON DUPLICATE KEY UPDATE token_value = VALUES(token_value),
                        token_owner = VALUES(token_owner),
                        expires_at  = VALUES(expires_at),
                        is_active   = VALUES(is_active);
