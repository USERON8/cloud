-- ==================== 用户数据库 (user_db) ====================
DROP DATABASE IF EXISTS `user_db`;
CREATE DATABASE IF NOT EXISTS `user_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `user_db`;

-- 用户表
CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT UNSIGNED PRIMARY KEY COMMENT '用户ID',
    username   VARCHAR(50)                        NOT NULL UNIQUE COMMENT '用户名',
    password   VARCHAR(255)                       NOT NULL COMMENT '加密密码',
    phone      VARCHAR(20) UNIQUE COMMENT '手机号',
    nickname   VARCHAR(50)                        NOT NULL COMMENT '昵称',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    email      VARCHAR(100) UNIQUE COMMENT '邮箱地址（用于GitHub登录）',
    github_id BIGINT UNSIGNED NULL COMMENT 'GitHub用户ID（OAuth登录专用）',
    github_username VARCHAR(100) NULL COMMENT 'GitHub用户名（OAuth登录专用）',
    oauth_provider VARCHAR(20) NULL COMMENT 'OAuth提供商（github, wechat等）',
    oauth_provider_id VARCHAR(100) NULL COMMENT 'OAuth提供商用户ID',
    status     TINYINT                            NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    user_type  ENUM ('USER', 'MERCHANT', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '用户类型',
    created_at DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT                            NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_user_type (user_type),
    INDEX idx_github_id (github_id),
    INDEX idx_github_username (github_username),
    INDEX idx_oauth_provider (oauth_provider),
    INDEX idx_oauth_provider_combined (oauth_provider, oauth_provider_id),
    UNIQUE INDEX uk_github_id (github_id),
    UNIQUE INDEX uk_github_username (github_username),
    UNIQUE INDEX uk_oauth_provider_id (oauth_provider, oauth_provider_id)
) COMMENT ='用户表';

-- 用户地址表
CREATE TABLE `user_address`
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '地址ID',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    consignee      VARCHAR(50)     NOT NULL COMMENT '收货人姓名',
    phone          VARCHAR(20)     NOT NULL COMMENT '联系电话',
    province       VARCHAR(20)     NOT NULL COMMENT '省份',
    city           VARCHAR(20)     NOT NULL COMMENT '城市',
    district       VARCHAR(20)     NOT NULL COMMENT '区县',
    street         VARCHAR(100)    NOT NULL COMMENT '街道',
    detail_address VARCHAR(255)    NOT NULL COMMENT '详细地址',
    is_default     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_user_id (user_id),
    INDEX idx_user_default (user_id, is_default),

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) COMMENT ='用户地址表';

-- 管理员表
CREATE TABLE `admin`
(
    id         BIGINT UNSIGNED PRIMARY KEY COMMENT '管理员ID',
    username   VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password   VARCHAR(255) NOT NULL COMMENT '加密密码',
    real_name  VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    phone      VARCHAR(20) COMMENT '联系电话',
    role       VARCHAR(20)  NOT NULL DEFAULT 'ADMIN' COMMENT '角色',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) COMMENT ='管理员表';

-- 商家表
CREATE TABLE `merchant`
(
    id            BIGINT UNSIGNED PRIMARY KEY COMMENT '商家ID',
    username      VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password      VARCHAR(255) NOT NULL COMMENT '加密密码',
    merchant_name VARCHAR(100) NOT NULL COMMENT '商家名称',
    phone         VARCHAR(20) COMMENT '联系电话',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_username (username),
    INDEX idx_status (status)
) COMMENT ='商家表';

-- 商家认证表
CREATE TABLE `merchant_auth`
(
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    merchant_id             BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
    business_license_number VARCHAR(50)     NOT NULL COMMENT '营业执照号码',
    business_license_url    VARCHAR(255)    NOT NULL COMMENT '营业执照图片URL',
    id_card_front_url       VARCHAR(255)    NOT NULL COMMENT '身份证正面URL',
    id_card_back_url        VARCHAR(255)    NOT NULL COMMENT '身份证反面URL',
    contact_phone           VARCHAR(20)     NOT NULL COMMENT '联系电话',
    contact_address         VARCHAR(255)    NOT NULL COMMENT '联系地址',
    auth_status             TINYINT         NOT NULL DEFAULT 0 COMMENT '认证状态：0-待审核，1-审核通过，2-审核拒绝',
    auth_remark             VARCHAR(255) COMMENT '审核备注',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    UNIQUE KEY uk_merchant_id (merchant_id),
    INDEX idx_auth_status (auth_status)
) COMMENT ='商家认证表';

-- 商家结算账户表
CREATE TABLE `merchant_settlement_account`
(
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    merchant_id    BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
    account_name   VARCHAR(100)    NOT NULL COMMENT '账户名称',
    account_number VARCHAR(50)     NOT NULL COMMENT '账户号码',
    account_type   TINYINT         NOT NULL COMMENT '账户类型：1-对公，2-对私',
    bank_name      VARCHAR(100)    NOT NULL COMMENT '开户银行',
    is_default     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否默认账户：0-否，1-是',
    status         TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_merchant_id (merchant_id),
    INDEX idx_is_default (is_default)
) COMMENT ='商家结算账户表';

-- 初始化超级管理员用户
INSERT INTO `admin` (id, username, password, real_name, phone, role)
VALUES (1, 'admin', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '超级管理员', '13800000000',
        'ADMIN');