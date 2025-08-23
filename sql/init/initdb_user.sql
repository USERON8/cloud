-- 创建库存数据库
CREATE DATABASE IF NOT EXISTS `user_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `user_db`;


-- 创建用户表
CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT PRIMARY KEY COMMENT '用户ID（雪花算法）',
    username   VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password   VARCHAR(255) NOT NULL COMMENT '加密密码',
    phone      CHAR(11) UNIQUE COMMENT '手机号（全局唯一）',
    nickname   VARCHAR(100) COMMENT '昵称',
    avatar_url VARCHAR(512) COMMENT '头像URL',
    status     TINYINT      NOT NULL              DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    user_type  ENUM ('USER', 'MERCHANT', 'ADMIN') DEFAULT 'USER' NOT NULL COMMENT '用户类型：1-普通用户，2-商家,3-管理员',
    deleted    TINYINT      NOT NULL              DEFAULT 0 COMMENT '软删除标记',
    created_at DATETIME                           DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME                           DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_at DATETIME                        DEFAULT NULL COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_user_type (user_type),
    -- 添加创建时间索引
    INDEX idx_created_at (created_at)
) COMMENT ='用户表';

-- 创建用户地址信息表
CREATE TABLE `user_address`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '地址ID（主键）',
    `user_id`        BIGINT       NOT NULL COMMENT '关联用户ID',
    `consignee`      VARCHAR(50)  NOT NULL COMMENT '收货人姓名',
    `phone`          VARCHAR(20)  NOT NULL COMMENT '联系电话',
    `province`       VARCHAR(20)  NOT NULL COMMENT '省份',
    `city`           VARCHAR(20)  NOT NULL COMMENT '城市',
    `district`       VARCHAR(20)  NOT NULL COMMENT '区县',
    `street`         VARCHAR(100) NOT NULL COMMENT '街道',
    `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址（门牌号）',
    `is_default`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否默认地址（0否1是）',
    `created_at`     DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    -- 优化索引以支持高并发查询
    INDEX `idx_user_id` (`user_id`),
    -- 添加用于默认地址查询的索引
    INDEX `idx_user_default` (`user_id`, `is_default`),
    -- 添加创建时间索引
    INDEX `idx_create_time` (`created_at`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户地址表';