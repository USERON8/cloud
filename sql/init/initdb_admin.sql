-- ==================== 管理员数据库 (admin_db) ====================
CREATE DATABASE IF NOT EXISTS `admin_db` DEFAULT CHARSET utf8mb4;
USE `admin_db`;

-- 管理员信息表（基于原用户表中的管理员信息）
CREATE TABLE `admin`
(
    `id`         BIGINT PRIMARY KEY COMMENT '管理员ID（与用户ID一致）',
    `username`   VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    `password`   VARCHAR(255) NOT NULL COMMENT '加密密码',
    `real_name`  VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    `phone`      VARCHAR(20) COMMENT '联系电话',
    `role`       VARCHAR(50)  NOT NULL DEFAULT 'ADMIN' COMMENT '角色：ADMIN-超级管理员，OPERATOR-普通管理员',
    `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    KEY `idx_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT ='管理员信息表';


-- 初始化超级管理员用户
INSERT INTO `admin` (`id`, `username`, `password`, `real_name`, `phone`, `role`)
VALUES (1, 'admin', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '超级管理员', '13800000000', 'ADMIN');