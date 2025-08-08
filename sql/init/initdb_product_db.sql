-- 创建产品数据库
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE product_db;

-- 店铺表（修复主键冲突 + 分区规则）
CREATE TABLE `shops`
(
    `shop_id`     BIGINT COMMENT '店铺ID',
    `merchant_id` BIGINT       NOT NULL COMMENT '商家ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '店铺名称',
    `status`      TINYINT DEFAULT 1 COMMENT '0-关闭，1-营业',
    `shard_key`   INT GENERATED ALWAYS AS (shop_id % 8) STORED COMMENT '分片键（8库）',
    -- 关键修复1：复合主键包含分区键
    PRIMARY KEY (`shop_id`, `shard_key`),
    KEY `idx_merchant` (`merchant_id`),
    KEY `idx_shard` (`shard_key`)
) ENGINE = InnoDB COMMENT ='店铺表'
    PARTITION BY RANGE (shard_key) (
        PARTITION p0 VALUES LESS THAN (2),
        PARTITION p1 VALUES LESS THAN (4),
        PARTITION p2 VALUES LESS THAN (6),
        PARTITION p3 VALUES LESS THAN (8)
        );

-- 商品表（移除外键 + 修复主键）
CREATE TABLE `products`
(
    `product_id`  BIGINT COMMENT '商品ID',
    `shop_id`     BIGINT         NOT NULL COMMENT '店铺ID',
    `name`        VARCHAR(200)   NOT NULL COMMENT '商品名称',
    `price`       DECIMAL(10, 2) NOT NULL COMMENT '售价',
    `category_id` INT COMMENT '分类ID',
    `status`      TINYINT DEFAULT 0 COMMENT '0-下架，1-上架',
    `shard_key`   INT GENERATED ALWAYS AS (shop_id % 8) STORED COMMENT '分片键',
    -- 关键修复2：复合主键 + 移除不支持的外键
    PRIMARY KEY (`product_id`, `shard_key`),
    KEY `idx_shard` (`shard_key`)
) ENGINE = InnoDB COMMENT ='商品表'
    PARTITION BY KEY (shard_key) PARTITIONS 8;
-- 按分片键分区

-- 插入初始数据（确保分片键对齐）
INSERT INTO `shops` (`shop_id`, `merchant_id`, `name`)
VALUES (6001, 5001, '旗舰店'), -- shard_key=6001%8=1 → p0分区
       (6002, 5001, '分店'); -- shard_key=6002%8=2 → p1分区

INSERT INTO `products` (`product_id`, `shop_id`, `name`, `price`)
VALUES (7001, 6001, '智能手机', 2999.00), -- shard_key=6001%8=1
       (7002, 6001, '蓝牙耳机', 399.00); -- shard_key=1（与店铺同分区）