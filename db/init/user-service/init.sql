DROP DATABASE IF EXISTS user_db;
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_db;

CREATE TABLE IF NOT EXISTS users
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    username          VARCHAR(50)  NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    phone             VARCHAR(20) UNIQUE,
    nickname          VARCHAR(50)  NOT NULL,
    avatar_url        VARCHAR(255),
    email             VARCHAR(100) UNIQUE,
    github_id         BIGINT UNSIGNED NULL UNIQUE,
    github_username   VARCHAR(100) NULL,
    oauth_provider    VARCHAR(32) NULL,
    oauth_provider_id VARCHAR(100) NULL,
    status            TINYINT      NOT NULL DEFAULT 1,
    user_type         ENUM ('USER', 'MERCHANT', 'ADMIN') NOT NULL DEFAULT 'USER',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT      NOT NULL DEFAULT 0,
    version           INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_oauth_provider_id (oauth_provider, oauth_provider_id),
    INDEX idx_user_status (status),
    INDEX idx_user_type (user_type)
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
    INDEX idx_user_address_user (user_id),
    INDEX idx_user_address_default (user_id, is_default)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin
(
    id         BIGINT UNSIGNED PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    real_name  VARCHAR(50)  NOT NULL,
    phone      VARCHAR(20),
    role       VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    status     TINYINT      NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    version    INT          NOT NULL DEFAULT 0,
    INDEX idx_admin_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant
(
    id            BIGINT UNSIGNED PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    status        TINYINT      NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    version       INT          NOT NULL DEFAULT 0,
    INDEX idx_merchant_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_auth
(
    id                      BIGINT UNSIGNED PRIMARY KEY,
    merchant_id             BIGINT UNSIGNED NOT NULL UNIQUE,
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
    INDEX idx_merchant_auth_status (auth_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
