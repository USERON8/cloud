-- ==================== 库存数据库初始化脚本 (stock_db) ====================
-- 包含: 库存主表、入库记录、出库记录、库存日志、库存盘点
-- 版本: v2.0
-- 更新时间: 2025-01-16
-- ==================================================================================

DROP DATABASE IF EXISTS `stock_db`;
CREATE DATABASE IF NOT EXISTS `stock_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `stock_db`;

-- ==================== 1. 库存主表 ====================
CREATE TABLE `stock` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `stock_quantity` INT NOT NULL DEFAULT 0 COMMENT '总库存量',
    `frozen_quantity` INT NOT NULL DEFAULT 0 COMMENT '冻结库存量',
    `stock_status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 2-缺货, 3-下架',
    `low_stock_threshold` INT DEFAULT 10 COMMENT '低库存预警阈值',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-未删除, 1-已删除',

    -- 唯一索引
    UNIQUE KEY `uk_product_id` (`product_id`),

    -- 常规索引
    INDEX `idx_stock_status` (`stock_status`),
    INDEX `idx_updated_at` (`updated_at`),

    -- 复合索引(优化查询性能)
    INDEX `idx_product_status` (`product_id`, `stock_status`),
    INDEX `idx_status_quantity` (`stock_status`, `stock_quantity`),
    INDEX `idx_threshold_alert` (`low_stock_threshold`, `stock_quantity`, `stock_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存主表';

-- ==================== 2. 入库记录表 ====================
CREATE TABLE `stock_in` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `quantity` INT NOT NULL COMMENT '入库数量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-未删除, 1-已删除',

    -- 常规索引
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_created_at` (`created_at`),

    -- 复合索引
    INDEX `idx_product_created` (`product_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库记录表';

-- ==================== 3. 出库记录表 ====================
CREATE TABLE `stock_out` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    `quantity` INT NOT NULL COMMENT '出库数量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-未删除, 1-已删除',

    -- 常规索引
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_created_at` (`created_at`),

    -- 复合索引
    INDEX `idx_product_created` (`product_id`, `created_at`),
    INDEX `idx_order_created` (`order_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库记录表';

-- ==================== 4. 库存操作日志表 ====================
CREATE TABLE `stock_log` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) COMMENT '商品名称',
    `operation_type` VARCHAR(20) NOT NULL COMMENT '操作类型: IN-入库, OUT-出库, RESERVE-预留, RELEASE-释放, ADJUST-调整, COUNT-盘点',
    `quantity_before` INT NOT NULL COMMENT '操作前库存数量',
    `quantity_after` INT NOT NULL COMMENT '操作后库存数量',
    `quantity_change` INT NOT NULL COMMENT '操作数量(变化量)',
    `order_id` BIGINT UNSIGNED COMMENT '关联订单ID',
    `order_no` VARCHAR(50) COMMENT '关联订单号',
    `operator_id` BIGINT UNSIGNED COMMENT '操作人ID',
    `operator_name` VARCHAR(100) COMMENT '操作人名称',
    `remark` VARCHAR(500) COMMENT '操作备注',
    `operate_time` DATETIME NOT NULL COMMENT '操作时间',
    `ip_address` VARCHAR(50) COMMENT '操作IP地址',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-未删除, 1-已删除',

    -- 常规索引
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_operation_type` (`operation_type`),
    INDEX `idx_operate_time` (`operate_time`),
    INDEX `idx_created_at` (`created_at`),

    -- 复合索引(优化查询性能)
    INDEX `idx_product_operate` (`product_id`, `operate_time`),
    INDEX `idx_type_operate` (`operation_type`, `operate_time`),
    INDEX `idx_order_operate` (`order_id`, `operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存操作日志表';

-- ==================== 5. 库存盘点表 ====================
CREATE TABLE `stock_count` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `count_no` VARCHAR(50) NOT NULL COMMENT '盘点单号',
    `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) COMMENT '商品名称',
    `expected_quantity` INT NOT NULL COMMENT '预期库存数量(系统记录)',
    `actual_quantity` INT NOT NULL COMMENT '实际库存数量(盘点结果)',
    `difference` INT NOT NULL COMMENT '库存差异(实际-预期)',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '盘点状态: PENDING-待确认, CONFIRMED-已确认, CANCELLED-已取消',
    `operator_id` BIGINT UNSIGNED COMMENT '盘点人ID',
    `operator_name` VARCHAR(100) COMMENT '盘点人名称',
    `confirm_user_id` BIGINT UNSIGNED COMMENT '确认人ID',
    `confirm_user_name` VARCHAR(100) COMMENT '确认人名称',
    `count_time` DATETIME NOT NULL COMMENT '盘点时间',
    `confirm_time` DATETIME COMMENT '确认时间',
    `remark` VARCHAR(500) COMMENT '盘点备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-未删除, 1-已删除',

    -- 唯一索引
    UNIQUE KEY `uk_count_no` (`count_no`),

    -- 常规索引
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_count_time` (`count_time`),
    INDEX `idx_created_at` (`created_at`),

    -- 复合索引(优化查询性能)
    INDEX `idx_product_count` (`product_id`, `count_time`),
    INDEX `idx_status_count` (`status`, `count_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存盘点表';

-- ==================== 初始化数据 ====================

-- 插入测试库存数据
INSERT INTO `stock` (`product_id`, `product_name`, `stock_quantity`, `frozen_quantity`, `stock_status`, `low_stock_threshold`) VALUES
(1, '华为Mate 60 Pro', 100, 0, 1, 20),
(2, '小米14 Ultra', 150, 10, 1, 30),
(3, 'iPhone 15 Pro Max', 80, 5, 1, 15),
(4, 'OPPO Find X7', 120, 0, 1, 25),
(5, 'vivo X100 Pro', 90, 0, 1, 20);

-- 插入测试入库记录
INSERT INTO `stock_in` (`product_id`, `quantity`) VALUES
(1, 100),
(2, 150),
(3, 80),
(4, 120),
(5, 90);

-- 插入测试出库记录
INSERT INTO `stock_out` (`product_id`, `order_id`, `quantity`) VALUES
(2, 1001, 10),
(3, 1002, 5);

-- 插入测试库存日志
INSERT INTO `stock_log` (`product_id`, `product_name`, `operation_type`, `quantity_before`, `quantity_after`, `quantity_change`, `remark`, `operate_time`, `operator_name`) VALUES
(1, '华为Mate 60 Pro', 'IN', 0, 100, 100, '初始入库', NOW(), '系统管理员'),
(2, '小米14 Ultra', 'IN', 0, 150, 150, '初始入库', NOW(), '系统管理员'),
(3, 'iPhone 15 Pro Max', 'IN', 0, 80, 80, '初始入库', NOW(), '系统管理员'),
(2, '小米14 Ultra', 'RESERVE', 150, 140, -10, '订单预留', NOW(), '系统'),
(3, 'iPhone 15 Pro Max', 'RESERVE', 80, 75, -5, '订单预留', NOW(), '系统');

-- 插入测试盘点记录
INSERT INTO `stock_count` (`count_no`, `product_id`, `product_name`, `expected_quantity`, `actual_quantity`, `difference`, `status`, `operator_name`, `count_time`) VALUES
('COUNT2025011600001', 1, '华为Mate 60 Pro', 100, 98, -2, 'PENDING', '仓库管理员', NOW()),
('COUNT2025011600002', 2, '小米14 Ultra', 140, 142, 2, 'CONFIRMED', '仓库管理员', NOW());

COMMIT;

-- ==================== 索引优化说明 ====================
-- 1. 唯一索引: uk_product_id, uk_count_no - 保证数据唯一性
-- 2. 单列索引: 用于单条件查询
-- 3. 复合索引: 优化多条件查询和排序
--    - idx_product_operate: 查询商品的操作历史
--    - idx_type_operate: 按操作类型统计
--    - idx_threshold_alert: 低库存预警查询优化
-- 4. 时间索引: 支持时间范围查询和统计
