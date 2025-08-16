-- 创建产品数据库
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE product_db;

-- 店铺表（简化结构）
CREATE TABLE `shops`
(
    `shop_id`     BIGINT PRIMARY KEY COMMENT '店铺ID',
    `merchant_id` BIGINT       NOT NULL COMMENT '商家ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '店铺名称',
    `status`      TINYINT DEFAULT 1 COMMENT '0-关闭，1-营业',
    KEY `idx_merchant` (`merchant_id`)
) ENGINE = InnoDB COMMENT = '店铺表';

-- 商品表（简化结构）
CREATE TABLE `products`
(
    `product_id`  BIGINT PRIMARY KEY COMMENT '商品ID',
    `shop_id`     BIGINT         NOT NULL COMMENT '店铺ID',
    `name`        VARCHAR(200)   NOT NULL COMMENT '商品名称',
    `price`       DECIMAL(10, 2) NOT NULL COMMENT '售价',
    `stock_count` INT            NOT NULL DEFAULT 0 COMMENT '库存数量',
    `category_id` INT COMMENT '分类ID',
    `status`      TINYINT DEFAULT 0 COMMENT '0-下架，1-上架',
    KEY `idx_shop` (`shop_id`)
) ENGINE = InnoDB COMMENT = '商品表';
-- 商品分类表（支持多级分类）
CREATE TABLE `category`
(
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    `parent_id`  BIGINT    DEFAULT 0 COMMENT '父分类ID（0=根分类）',
    `name`       VARCHAR(50) NOT NULL COMMENT '分类名称',
    `level`      TINYINT     NOT NULL COMMENT '层级（1=一级分类）',
    `status`     TINYINT   DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB COMMENT ='商品分类表';

-- 插入初始数据
INSERT INTO `shops` (`shop_id`, `merchant_id`, `name`)
VALUES (6001, 5001, '旗舰店'),
       (6002, 5001, '分店');

INSERT INTO `products` (`product_id`, `shop_id`, `name`, `price`, `stock_count`)
VALUES (7001, 6001, '智能手机', 2999.00, 100),
       (7002, 6001, '蓝牙耳机', 399.00, 200);