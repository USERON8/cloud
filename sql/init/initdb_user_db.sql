-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY COMMENT '用户ID（雪花算法）',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '加密密码',
    user_type VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色类型：ADMIN/MERCHANT/USER',
    email VARCHAR(100) UNIQUE COMMENT '邮箱（全局唯一）',
    phone VARCHAR(20) UNIQUE COMMENT '手机号（全局唯一）',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    avatar_file_name VARCHAR(255) COMMENT '头像文件名',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_user_type (user_type),
    INDEX idx_status (status)
) COMMENT='用户表';

-- 创建商家认证信息表
CREATE TABLE IF NOT EXISTS merchant_auth (
    id BIGINT PRIMARY KEY COMMENT '认证ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    shop_name VARCHAR(100) NOT NULL COMMENT '店铺名称',
    business_license_number VARCHAR(100) NOT NULL COMMENT '营业执照号码',
    business_license_url VARCHAR(255) NOT NULL COMMENT '营业执照图片URL',
    id_card_front_url VARCHAR(255) NOT NULL COMMENT '身份证正面URL',
    id_card_back_url VARCHAR(255) NOT NULL COMMENT '身份证反面URL',
    contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    contact_address VARCHAR(255) NOT NULL COMMENT '联系地址',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-审核通过，2-审核拒绝',
    audit_remark VARCHAR(255) COMMENT '审核备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) COMMENT='商家认证信息表';

-- 初始化管理员用户
INSERT INTO users (user_id, username, password_hash, user_type, email, phone, nickname, status) VALUES
(1, 'admin', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', 'ADMIN', 'admin@example.com', '13800000000', '超级管理员', 1)
ON DUPLICATE KEY UPDATE username=username;

-- 初始化测试商家用户
INSERT INTO users (user_id, username, password_hash, user_type, email, phone, nickname, status) VALUES
(2, 'merchant', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', 'MERCHANT', 'merchant@example.com', '13800000001', '测试商家', 0)
ON DUPLICATE KEY UPDATE username=username;