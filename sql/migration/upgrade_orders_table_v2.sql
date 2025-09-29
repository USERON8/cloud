-- ==================== 订单表结构升级迁移脚本 ====================
-- 版本: v2.0
-- 创建时间: 2025-09-26
-- 说明: 为orders表添加新的业务字段和BaseEntity标准字段
-- 
-- 使用方法:
-- 1. 在执行前备份orders表数据
-- 2. 在order_db数据库中执行此脚本
-- 3. 验证数据完整性

USE `order_db`;

-- 检查表是否存在
SELECT 
    TABLE_NAME, 
    TABLE_COMMENT 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders';

-- 1. 为orders表添加新字段
ALTER TABLE `orders` 
ADD COLUMN `order_no` VARCHAR(32) NULL COMMENT '订单号（业务唯一编号）' AFTER `id`,
ADD COLUMN `pay_time` DATETIME NULL COMMENT '支付时间' AFTER `address_id`,
ADD COLUMN `ship_time` DATETIME NULL COMMENT '发货时间' AFTER `pay_time`,
ADD COLUMN `complete_time` DATETIME NULL COMMENT '完成时间' AFTER `ship_time`,
ADD COLUMN `cancel_time` DATETIME NULL COMMENT '取消时间' AFTER `complete_time`,
ADD COLUMN `cancel_reason` VARCHAR(255) NULL COMMENT '取消原因' AFTER `cancel_time`,
ADD COLUMN `remark` VARCHAR(500) NULL COMMENT '备注' AFTER `cancel_reason`;

-- 2. 添加BaseEntity标准字段（如果不存在）
-- 检查是否已有create_by字段，没有则添加
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders' 
  AND COLUMN_NAME = 'create_by';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `orders` ADD COLUMN `create_by` BIGINT UNSIGNED NULL COMMENT ''创建人'' AFTER `remark`',
    'SELECT ''create_by字段已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加update_by字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders' 
  AND COLUMN_NAME = 'update_by';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `orders` ADD COLUMN `update_by` BIGINT UNSIGNED NULL COMMENT ''更新人'' AFTER `create_by`',
    'SELECT ''update_by字段已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加version字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders' 
  AND COLUMN_NAME = 'version';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `orders` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `update_by`',
    'SELECT ''version字段已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 重命名时间字段以符合BaseEntity标准（如果需要）
-- 检查是否需要重命名created_at为create_time
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders' 
  AND COLUMN_NAME = 'created_at';

SET @sql = IF(@col_exists > 0,
    'ALTER TABLE `orders` CHANGE COLUMN `created_at` `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''',
    'SELECT ''created_at字段不存在或已重命名'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查是否需要重命名updated_at为update_time
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders' 
  AND COLUMN_NAME = 'updated_at';

SET @sql = IF(@col_exists > 0,
    'ALTER TABLE `orders` CHANGE COLUMN `updated_at` `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''',
    'SELECT ''updated_at字段不存在或已重命名'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 为现有订单生成订单号（如果order_no为空）
UPDATE `orders` 
SET `order_no` = CONCAT('ORD', UNIX_TIMESTAMP(create_time) * 1000, LPAD(id % 1000, 3, '0'))
WHERE `order_no` IS NULL OR `order_no` = '';

-- 5. 设置order_no为NOT NULL并添加唯一约束
ALTER TABLE `orders` 
MODIFY COLUMN `order_no` VARCHAR(32) NOT NULL COMMENT '订单号（业务唯一编号）';

-- 添加唯一索引（如果不存在）
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders' 
  AND INDEX_NAME = 'uk_order_no';

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE `orders` ADD UNIQUE KEY `uk_order_no` (`order_no`)',
    'SELECT ''uk_order_no索引已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. 添加其他索引（如果不存在）
-- 支付时间索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders' 
  AND INDEX_NAME = 'idx_pay_time';

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE `orders` ADD INDEX `idx_pay_time` (`pay_time`)',
    'SELECT ''idx_pay_time索引已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 发货时间索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders' 
  AND INDEX_NAME = 'idx_ship_time';

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE `orders` ADD INDEX `idx_ship_time` (`ship_time`)',
    'SELECT ''idx_ship_time索引已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 完成时间索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders' 
  AND INDEX_NAME = 'idx_complete_time';

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE `orders` ADD INDEX `idx_complete_time` (`complete_time`)',
    'SELECT ''idx_complete_time索引已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 取消时间索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders' 
  AND INDEX_NAME = 'idx_cancel_time';

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE `orders` ADD INDEX `idx_cancel_time` (`cancel_time`)',
    'SELECT ''idx_cancel_time索引已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7. 验证表结构
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders'
ORDER BY ORDINAL_POSITION;

-- 8. 验证索引
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'orders'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 迁移完成提示
SELECT '订单表结构升级完成！' AS message, NOW() AS completed_time;
