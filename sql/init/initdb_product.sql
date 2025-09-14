-- ==================== 商品数据库 (product_db) ====================
DROP DATABASE IF EXISTS `product_db`;
CREATE DATABASE IF NOT EXISTS `product_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `product_db`;

-- 商品表
CREATE TABLE `products`
(
    id             BIGINT UNSIGNED PRIMARY KEY COMMENT '商品ID',
    shop_id        BIGINT UNSIGNED NOT NULL COMMENT '店铺ID',
    product_name   VARCHAR(100)    NOT NULL COMMENT '商品名称',
    price          DECIMAL(10, 2)  NOT NULL COMMENT '售价',
    stock_quantity INT             NOT NULL DEFAULT 0 COMMENT '库存数量',
    category_id    BIGINT UNSIGNED COMMENT '分类ID',
    status         TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-下架，1-上架',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_shop_id (shop_id),
    INDEX idx_status (status),
    INDEX idx_category_id (category_id)
) COMMENT ='商品表';

-- 商品分类表
CREATE TABLE `category`
(
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    parent_id  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID',
    name       VARCHAR(50)     NOT NULL COMMENT '分类名称',
    level      TINYINT         NOT NULL COMMENT '层级',
    status     TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status),
    INDEX idx_level (level)
) COMMENT ='商品分类表';

-- 商家店铺表
CREATE TABLE `merchant_shop`
(
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '店铺ID',
    merchant_id   BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
    shop_name     VARCHAR(100)    NOT NULL COMMENT '店铺名称',
    avatar_url    VARCHAR(255) COMMENT '店铺头像URL',
    description   TEXT COMMENT '店铺描述',
    contact_phone VARCHAR(20)     NOT NULL COMMENT '客服电话',
    address       VARCHAR(255)    NOT NULL COMMENT '详细地址',
    status        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-关闭，1-营业',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_merchant_id (merchant_id),
    INDEX idx_status (status)
) COMMENT ='商家店铺表';