-- ==================== 订单明细表结构升级迁移脚本 ====================
-- 版本: v2.0  
-- 创建时间: 2025-09-26
-- 说明: 为order_item表字段名统一为BaseEntity标准
-- 
-- 使用方法:
-- 1. 在执行前备份order_item表数据
-- 2. 在order_db数据库中执行此脚本  
-- 3. 验证数据完整性

USE `order_db`;

-- 检查表是否存在
SELECT 
    TABLE_NAME, 
    TABLE_COMMENT 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'order_item';

-- 1. 重命名时间字段以符合BaseEntity标准（如果需要）
-- 检查是否需要重命名created_at为create_time
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'order_item' 
  AND COLUMN_NAME = 'created_at';

SET @sql = IF(@col_exists > 0,
    'ALTER TABLE `order_item` CHANGE COLUMN `created_at` `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''',
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
  AND TABLE_NAME = 'order_item' 
  AND COLUMN_NAME = 'updated_at';

SET @sql = IF(@col_exists > 0,
    'ALTER TABLE `order_item` CHANGE COLUMN `updated_at` `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''',
    'SELECT ''updated_at字段不存在或已重命名'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 添加BaseEntity标准字段（如果不存在）
-- 检查是否已有create_by字段，没有则添加
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'order_item' 
  AND COLUMN_NAME = 'create_by';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `order_item` ADD COLUMN `create_by` BIGINT UNSIGNED NULL COMMENT ''创建人'' AFTER `price`',
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
  AND TABLE_NAME = 'order_item' 
  AND COLUMN_NAME = 'update_by';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `order_item` ADD COLUMN `update_by` BIGINT UNSIGNED NULL COMMENT ''更新人'' AFTER `create_by`',
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
  AND TABLE_NAME = 'order_item' 
  AND COLUMN_NAME = 'version';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `order_item` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `update_by`',
    'SELECT ''version字段已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 更新deleted字段注释以符合标准
ALTER TABLE `order_item` 
MODIFY COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除,1-已删除';

-- 4. 添加create_time索引（如果不存在）
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'order_item' 
  AND INDEX_NAME = 'idx_create_time';

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE `order_item` ADD INDEX `idx_create_time` (`create_time`)',
    'SELECT ''idx_create_time索引已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 验证表结构
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'order_item'
ORDER BY ORDINAL_POSITION;

-- 6. 验证索引
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'order_db' 
  AND TABLE_NAME = 'order_item'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 迁移完成提示
SELECT '订单明细表结构升级完成！' AS message, NOW() AS completed_time;
