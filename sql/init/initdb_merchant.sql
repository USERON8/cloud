-- ==================== 商家数据库 (merchant_db) ====================
CREATE DATABASE IF NOT EXISTS `merchant_db` DEFAULT CHARSET utf8mb4;
USE `merchant_db`;

-- 商家信息表（简化版）
CREATE TABLE `merchant`
(
    `id`            BIGINT PRIMARY KEY COMMENT '商家ID（与用户ID一致）',
    `username`      VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    `password`      VARCHAR(255) NOT NULL COMMENT '加密密码',
    `merchant_name` VARCHAR(100) NOT NULL COMMENT '商家名称',
    `phone`         VARCHAR(20) COMMENT '联系电话',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `user_type`     VARCHAR(20)  NOT NULL DEFAULT 'MERCHANT' COMMENT '用户类型：固定为MERCHANT',
    `created_at`    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    KEY `idx_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_user_type` (`user_type`)
) ENGINE = InnoDB COMMENT ='商家信息表';

-- 商家认证信息表（简化版）
CREATE TABLE `merchant_auth`
(
    `id`                      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `merchant_id`             BIGINT       NOT NULL COMMENT '商家ID',
    `business_license_number` VARCHAR(100) NOT NULL COMMENT '营业执照号码',
    `business_license_url`    VARCHAR(255) NOT NULL COMMENT '营业执照图片URL',
    `id_card_front_url`       VARCHAR(255) NOT NULL COMMENT '身份证正面URL',
    `id_card_back_url`        VARCHAR(255) NOT NULL COMMENT '身份证反面URL',
    `contact_phone`           VARCHAR(20)  NOT NULL COMMENT '联系电话',
    `contact_address`         VARCHAR(255) NOT NULL COMMENT '联系地址',
    `auth_status`             TINYINT      NOT NULL DEFAULT 0 COMMENT '认证状态：0-待审核，1-审核通过，2-审核拒绝',
    `auth_remark`             VARCHAR(255) COMMENT '审核备注',
    `created_at`              DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`              DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                 TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    UNIQUE KEY `uk_merchant_id` (`merchant_id`),
    KEY `idx_auth_status` (`auth_status`)
) ENGINE = InnoDB COMMENT ='商家认证信息表';

-- 商家店铺信息表（简化版）
CREATE TABLE `merchant_shop`
(
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '店铺ID',
    `merchant_id`   BIGINT       NOT NULL COMMENT '商家ID',
    `shop_name`     VARCHAR(100) NOT NULL COMMENT '店铺名称',
    avatar_url      VARCHAR(255) COMMENT '店铺头像URL',
    `description`   TEXT COMMENT '店铺描述',
    `contact_phone` VARCHAR(20)  NOT NULL COMMENT '客服电话',
    `address`       VARCHAR(255) NOT NULL COMMENT '详细地址',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-关闭，1-营业，2-暂停营业',
    `created_at`    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT ='商家店铺信息表';

-- 商家结算账户表（简化版）
CREATE TABLE `merchant_settlement_account`
(
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `merchant_id`    BIGINT       NOT NULL COMMENT '商家ID',
    `account_name`   VARCHAR(100) NOT NULL COMMENT '账户名称',
    `account_number` VARCHAR(50)  NOT NULL COMMENT '账户号码',
    `account_type`   TINYINT      NOT NULL COMMENT '账户类型：1-对公账户，2-对私账户',
    `bank_name`      VARCHAR(100) NOT NULL COMMENT '开户银行',
    `is_default`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认账户：0-否，1-是',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `created_at`     DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_is_default` (`is_default`)
) ENGINE = InnoDB COMMENT ='商家结算账户表';