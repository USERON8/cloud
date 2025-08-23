-- 创建产品数据库
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE product_db;

-- 商品表（简化结构）
CREATE TABLE `products`
(
    `id`             BIGINT PRIMARY KEY COMMENT '商品ID',
    `shop_id`        BIGINT         NOT NULL COMMENT '店铺ID（引用merchant_db.merchant_shop表的shop_id）',
    `name`           VARCHAR(200)   NOT NULL COMMENT '商品名称',
    `price`          DECIMAL(10, 2) NOT NULL COMMENT '售价',
    `stock_quantity` INT            NOT NULL DEFAULT 0 COMMENT '库存数量',
    `category_id`    INT COMMENT '分类ID',
    `status`         TINYINT                 DEFAULT 0 COMMENT '0-下架，1-上架',
    `created_at`     DATETIME                DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    -- 优化索引以支持高并发查询
    KEY `idx_shop` (`shop_id`),
    -- 添加用于商品状态查询的索引
    KEY `idx_status` (`status`),
    -- 添加用于商品分类查询的索引
    KEY `idx_category` (`category_id`),
    -- 添加用于店铺商品查询的复合索引
    KEY `idx_shop_status` (`shop_id`, `status`),
    -- 添加用于分类商品查询的复合索引
    KEY `idx_category_status` (`category_id`, `status`),
    -- 添加创建时间索引
    KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB COMMENT = '商品表';

-- 商品分类表（支持多级分类）
CREATE TABLE `category`
(
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    `parent_id`  BIGINT               DEFAULT 0 COMMENT '父分类ID（0=根分类）',
    `name`       VARCHAR(50) NOT NULL COMMENT '分类名称',
    `level`      TINYINT     NOT NULL COMMENT '层级（1=一级分类）',
    `status`     TINYINT              DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `created_at` DATETIME             DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`  BIGINT COMMENT '创建人ID',
    `update_by`  BIGINT COMMENT '更新人ID',
    `deleted`    TINYINT     NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    -- 优化索引以支持高并发查询
    KEY `idx_parent_id` (`parent_id`),
    -- 添加用于分类状态查询的索引
    KEY `idx_status` (`status`),
    -- 添加用于层级查询的索引
    KEY `idx_level` (`level`),
    -- 添加用于父分类和状态查询的复合索引
    KEY `idx_parent_status` (`parent_id`, `status`)
) ENGINE = InnoDB COMMENT ='商品分类表';