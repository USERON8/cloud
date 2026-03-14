DROP DATABASE IF EXISTS user_db;
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_db;

CREATE TABLE IF NOT EXISTS users
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    username          VARCHAR(50)  NOT NULL,
    phone             VARCHAR(20)  NULL,
    nickname          VARCHAR(50)  NOT NULL,
    avatar_url        VARCHAR(255) NULL,
    email             VARCHAR(100) NULL,
    status            TINYINT      NOT NULL DEFAULT 1,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT      NOT NULL DEFAULT 0,
    version           INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_users_username_deleted (username, deleted),
    INDEX idx_users_deleted (deleted),
    UNIQUE KEY uk_users_phone (phone),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_status_deleted (status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_profile_ext
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    user_id           BIGINT UNSIGNED NOT NULL,
    gender            VARCHAR(16)     NULL,
    birthday          DATE            NULL,
    bio               VARCHAR(500)    NULL,
    country           VARCHAR(64)     NULL,
    province          VARCHAR(64)     NULL,
    city              VARCHAR(64)     NULL,
    personal_tags     JSON            NULL,
    preferences       JSON            NULL,
    last_login_at     DATETIME        NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_profile_ext_user_id (user_id),
    INDEX idx_user_profile_ext_city_deleted (city, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_address
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    user_id          BIGINT UNSIGNED NOT NULL,
    address_tag      VARCHAR(32)     NULL,
    receiver_name    VARCHAR(50)     NOT NULL,
    receiver_phone   VARCHAR(20)     NOT NULL,
    country          VARCHAR(64)     NOT NULL DEFAULT 'China',
    province         VARCHAR(64)     NOT NULL,
    city             VARCHAR(64)     NOT NULL,
    district         VARCHAR(64)     NOT NULL,
    street           VARCHAR(100)    NOT NULL,
    detail_address   VARCHAR(255)    NOT NULL,
    postal_code      VARCHAR(16)     NULL,
    longitude        DECIMAL(11, 7)  NULL,
    latitude         DECIMAL(11, 7)  NULL,
    is_default       TINYINT         NOT NULL DEFAULT 0,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    INDEX idx_user_address_user_deleted (user_id, deleted),
    INDEX idx_user_address_user_default_deleted (user_id, is_default, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_favorite
(
    id               BIGINT UNSIGNED PRIMARY KEY,
    user_id          BIGINT UNSIGNED NOT NULL,
    spu_id           BIGINT UNSIGNED NOT NULL,
    sku_id           BIGINT UNSIGNED NULL,
    favorite_status  VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT         NOT NULL DEFAULT 0,
    version          INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_favorite_user_spu_sku (user_id, spu_id, sku_id),
    INDEX idx_user_favorite_user_status_deleted (user_id, favorite_status, deleted),
    INDEX idx_user_favorite_spu_deleted (spu_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin
(
    id         BIGINT UNSIGNED PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    real_name  VARCHAR(50)  NOT NULL,
    phone      VARCHAR(20)  NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    status     TINYINT      NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    version    INT          NOT NULL DEFAULT 0,
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
    id                BIGINT UNSIGNED PRIMARY KEY,
    operator_id       BIGINT UNSIGNED NOT NULL,
    operator_role     VARCHAR(32)     NOT NULL,
    action            VARCHAR(128)    NOT NULL,
    target_type       VARCHAR(64)     NOT NULL,
    target_id         VARCHAR(64)     NOT NULL,
    trace_id          VARCHAR(64)     NULL,
    request_uri       VARCHAR(255)    NULL,
    request_method    VARCHAR(16)     NULL,
    request_payload   TEXT            NULL,
    result_code       INT             NOT NULL DEFAULT 0,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    INDEX idx_operation_audit_operator_created_deleted (operator_id, created_at, deleted),
    INDEX idx_operation_audit_target_deleted (target_type, target_id, deleted)
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

CREATE TABLE IF NOT EXISTS test_access_token
(
    id            BIGINT UNSIGNED PRIMARY KEY,
    token_value   VARCHAR(255)  NOT NULL,
    token_owner   VARCHAR(64)   NOT NULL DEFAULT 'test-user',
    expires_at    DATETIME      NOT NULL,
    is_active     TINYINT       NOT NULL DEFAULT 1,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_test_access_token_value (token_value),
    INDEX idx_test_access_token_active_expires (is_active, expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO test_access_token (id, token_value, token_owner, expires_at, is_active)
VALUES (1, 'TEST_ENV_PERMANENT_TOKEN', 'test-user', '2099-12-31 23:59:59', 1)
ON DUPLICATE KEY UPDATE
    token_value = VALUES(token_value),
    token_owner = VALUES(token_owner),
    expires_at = VALUES(expires_at),
    is_active = VALUES(is_active);

CREATE TABLE undo_log (
  branch_id BIGINT NOT NULL,
  xid VARCHAR(128) NOT NULL,
  context VARCHAR(128) NOT NULL,
  rollback_info LONGBLOB NOT NULL,
  log_status INT NOT NULL,
  log_created DATETIME NOT NULL,
  log_modified DATETIME NOT NULL,
  UNIQUE KEY ux_undo_log (xid,branch_id),
  INDEX idx_undo_log_xid (xid),
  INDEX idx_undo_log_branch (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
