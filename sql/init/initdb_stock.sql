-- ==================== 库存数据库 (stock_db) ====================
DROP DATABASE IF EXISTS `stock_db`;
CREATE DATABASE IF NOT EXISTS `stock_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `stock_db`;

-- 库存主表
CREATE TABLE `stock`
(
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    product_id      BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    product_name    VARCHAR(100)    NOT NULL COMMENT '商品名称',
    stock_quantity  INT             NOT NULL DEFAULT 0 COMMENT '总库存量',
    frozen_quantity INT             NOT NULL DEFAULT 0 COMMENT '冻结库存量',
    stock_status    TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-缺货，3-下架',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    -- 基础索引
    UNIQUE KEY uk_product_id (product_id),
    INDEX idx_stock_status (stock_status),

    -- 性能优化索引
    INDEX idx_product_status (product_id, stock_status),
    INDEX idx_status_quantity (stock_status, stock_quantity),
    INDEX idx_updated_status (updated_at, stock_status)
) COMMENT ='库存主表';

-- 入库记录表
CREATE TABLE `stock_in`
(
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    quantity   INT             NOT NULL COMMENT '入库数量',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    -- 基础索引
    INDEX idx_product_id (product_id),

    -- 性能优化索引
    INDEX idx_created_at (created_at)
) COMMENT ='入库记录表';

-- 出库记录表
CREATE TABLE `stock_out`
(
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    order_id   BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    quantity   INT             NOT NULL COMMENT '出库数量',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    -- 基础索引
    INDEX idx_product_id (product_id),
    INDEX idx_order_id (order_id),

    -- 性能优化索引
    INDEX idx_created_at (created_at)
) COMMENT ='出库记录表';