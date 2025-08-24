-- 创建库存数据库
CREATE DATABASE IF NOT EXISTS `stock_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `stock_db`;

-- 库存主表（动态计算可用库存）
CREATE TABLE IF NOT EXISTS `stock`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `product_id`      BIGINT       NOT NULL COMMENT '商品ID',
    product_name      VARCHAR(100) NOT NULL COMMENT '商品名称',
    `stock_quantity`  INT          NOT NULL DEFAULT 0 COMMENT '总库存量',
    `frozen_quantity` INT          NOT NULL DEFAULT 0 COMMENT '冻结库存量',
    `stock_status`    INT          NOT NULL DEFAULT 1 COMMENT '库存状态：1-正常，2-缺货，3-下架',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at`      DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product` (`product_id`),
    -- 添加用于高并发读取和库存变更操作的复合索引
    KEY `idx_product_counts` (`product_id`, `stock_quantity`, `frozen_quantity`)
) ENGINE = InnoDB COMMENT ='库存主表（支持高并发）';

-- 入库记录表
CREATE TABLE IF NOT EXISTS `stock_in`
(
    `id`         BIGINT  NOT NULL AUTO_INCREMENT,
    `product_id` BIGINT  NOT NULL,
    `quantity`   INT     NOT NULL COMMENT '入库数量',
    `created_at` DATETIME         DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    -- 优化索引以支持高并发查询
    KEY `idx_product_time` (`product_id`, `created_at`),
    KEY `idx_create_time` (`created_at`)
) ENGINE = InnoDB COMMENT ='入库明细表';

-- 出库记录表
CREATE TABLE IF NOT EXISTS `stock_out`
(
    `id`         BIGINT  NOT NULL AUTO_INCREMENT,
    `product_id` BIGINT  NOT NULL,
    `quantity`   INT     NOT NULL COMMENT '出库数量',
    `order_id`   BIGINT COMMENT '关联订单ID',
    `created_at` DATETIME         DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    -- 优化索引以支持高并发查询
    KEY `idx_order` (`order_id`),
    KEY `idx_product_time` (`product_id`, `created_at`),
    KEY `idx_create_time` (`created_at`)
) ENGINE = InnoDB COMMENT ='出库明细表';