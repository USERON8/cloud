-- 创建用户数据库
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE user_db;

-- 用户表（移除分片键和分区）
CREATE TABLE `users`
(
    `user_id`          BIGINT PRIMARY KEY COMMENT '用户ID（雪花算法）', -- 主键简化
    `username`         VARCHAR(50)                        NOT NULL COMMENT '用户名',
    `password_hash`    VARCHAR(255)                       NOT NULL COMMENT '加密密码',
    `user_type`        ENUM ('ADMIN', 'MERCHANT', 'USER') NOT NULL COMMENT '角色类型',
    `email`            VARCHAR(100) UNIQUE COMMENT '邮箱（全局唯一）',  -- 唯一索引独立
    `phone`            VARCHAR(20) UNIQUE COMMENT '手机号（全局唯一）',
    `nickname`         VARCHAR(50) COMMENT '昵称',
    `avatar_url`       VARCHAR(255) COMMENT '头像URL',
    `avatar_file_name` VARCHAR(255) COMMENT '头像文件名',
    `status`           TINYINT   DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted`          TINYINT   DEFAULT 0 COMMENT '软删除标记',
    `created_at`       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB COMMENT '用户表';
-- 商家表（移除分片键和分区，添加外键关联）
CREATE TABLE `merchants`
(
    `merchant_id`  BIGINT PRIMARY KEY COMMENT '商家ID',
    `user_id`      BIGINT NOT NULL COMMENT '关联用户ID',
    `shop_count`   INT     DEFAULT 0 COMMENT '店铺数量',
    `audit_status` TINYINT DEFAULT 0 COMMENT '审核状态：0-待审，1-已认证',
    FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) -- 添加外键约束
) ENGINE = InnoDB COMMENT '商家信息表';
-- 插入用户数据（无需分片键）
INSERT INTO `users` (`user_id`, `username`, `password_hash`, `user_type`, `email`)
VALUES (1001, 'admin', '$2a$10$w95Y6.Y0cI64Qzy./4J7FO/TjA2RbBQ58O94gUmB1ZIYD4NfB9z02', 'ADMIN', 'admin@example.com'),
       (2001, 'merchant1', '$2a$10$w95Y6.Y0cI64Qzy./4J7FO/TjA2RbBQ58O94gUmB1ZIYD4NfB9z02', 'MERCHANT',
        'm1@example.com'),
       (3001, 'user1', '$2a$10$w95Y6.Y0cI64Qzy./4J7FO/TjA2RbBQ58O94gUmB1ZIYD4NfB9z02', 'USER', 'user1@example.com');

-- 插入商家数据（直接关联user_id）
INSERT INTO `merchants` (`merchant_id`, `user_id`, `audit_status`)
VALUES (5001, 2001, 1);