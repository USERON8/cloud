DROP DATABASE IF EXISTS user_db;
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_db;

CREATE TABLE IF NOT EXISTS users
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    username          VARCHAR(50)  NOT NULL,
    password          VARCHAR(255) NOT NULL,
    phone             VARCHAR(20),
    nickname          VARCHAR(50)  NOT NULL,
    avatar_url        VARCHAR(255),
    email             VARCHAR(100),
    github_id         BIGINT UNSIGNED NULL,
    github_username   VARCHAR(100) NULL,
    oauth_provider    VARCHAR(32) NULL,
    oauth_provider_id VARCHAR(100) NULL,
    status            TINYINT      NOT NULL DEFAULT 1,
    user_type         ENUM ('USER', 'MERCHANT', 'ADMIN') NOT NULL DEFAULT 'USER',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT      NOT NULL DEFAULT 0,
    version           INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_phone (phone),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_github_id (github_id),
    UNIQUE KEY uk_users_oauth_provider_id (oauth_provider, oauth_provider_id),
    INDEX idx_users_status_deleted (status, deleted),
    INDEX idx_users_user_type_deleted (user_type, deleted),
    INDEX idx_users_github_username_deleted (github_username, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_address
(
    id             BIGINT UNSIGNED PRIMARY KEY,
    user_id        BIGINT UNSIGNED NOT NULL,
    consignee      VARCHAR(50)     NOT NULL,
    phone          VARCHAR(20)     NOT NULL,
    province       VARCHAR(20)     NOT NULL,
    city           VARCHAR(20)     NOT NULL,
    district       VARCHAR(20)     NOT NULL,
    street         VARCHAR(100)    NOT NULL,
    detail_address VARCHAR(255)    NOT NULL,
    is_default     TINYINT         NOT NULL DEFAULT 0,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    version        INT             NOT NULL DEFAULT 0,
    INDEX idx_user_address_user_id_deleted (user_id, deleted),
    INDEX idx_user_address_user_default_deleted (user_id, is_default, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin
(
    id         BIGINT UNSIGNED PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    real_name  VARCHAR(50)  NOT NULL,
    phone      VARCHAR(20),
    role       VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    status     TINYINT      NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    version    INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_admin_username (username),
    INDEX idx_admin_status_deleted (status, deleted),
    INDEX idx_admin_role_deleted (role, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant
(
    id            BIGINT UNSIGNED PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password      VARCHAR(255) NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    status        TINYINT      NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    version       INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_merchant_username (username),
    INDEX idx_merchant_status_deleted (status, deleted)
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
    auth_remark             VARCHAR(255),
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                 TINYINT         NOT NULL DEFAULT 0,
    version                 INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_merchant_auth_merchant_id (merchant_id),
    INDEX idx_merchant_auth_status_deleted (auth_status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS test_access_token
(
    id            BIGINT UNSIGNED PRIMARY KEY,
    token_value   VARCHAR(1024) NOT NULL,
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
