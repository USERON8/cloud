-- 创建产品数据库
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE product_db;

-- 店铺表（简化结构）
CREATE TABLE `shops`
(
`shop_id`     BIGINT PRIMARY KEY COMMENT '店铺ID',
`merchant_id` BIGINT NOT NULL COMMENT '商家ID',
`name`        VARCHAR(100) NOT NULL COMMENT '店铺名称',
`status`      TINYINT DEFAULT 1 COMMENT '0-关闭，1-营业',
KEY `idx_merchant` (`merchant_id`)
) ENGINE = InnoDB COMMENT = '店铺表';

-- 商品表（简化结构）
CREATE TABLE `products`
(
`product_id`  BIGINT PRIMARY KEY COMMENT '商品ID',
`shop_id`     BIGINT NOT NULL COMMENT '店铺ID',
`name`        VARCHAR(200)   NOT NULL COMMENT '商品名称',
`price`       DECIMAL(10, 2) NOT NULL COMMENT '售价',
`category_id` INT COMMENT '分类ID',
`status`      TINYINT DEFAULT 0 COMMENT '0-下架，1-上架',
KEY `idx_shop` (`shop_id`)
) ENGINE = InnoDB COMMENT = '商品表';
-- 商品分类表（支持多级分类）
CREATE TABLE `category`
(
`id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
`parent_id`  BIGINT DEFAULT 0 COMMENT '父分类ID（0=根分类）',
`name`       VARCHAR(50) NOT NULL COMMENT '分类名称',
`level`      TINYINT NOT NULL COMMENT '层级（1=一级分类）',
`status`     TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB COMMENT ='商品分类表';

-- 插入初始数据
INSERT INTO `shops` (`shop_id`, `merchant_id`, `name`)
VALUES (6001, 5001, '旗舰店'),
(6002, 5001, '分店');

INSERT INTO `products` (`product_id`, `shop_id`, `name`, `price`)
VALUES (7001, 6001, '智能手机', 2999.00),
(7002, 6001, '蓝牙耳机', 399.00);
-- 创建库存数据库
CREATE DATABASE IF NOT EXISTS `stock_db`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;
USE `stock_db`;

-- 库存主表（动态计算可用库存）
CREATE TABLE IF NOT EXISTS `stock`
(
`id`              BIGINT NOT NULL AUTO_INCREMENT,
`product_id`      BIGINT NOT NULL COMMENT '商品ID',
`stock_count`     INT NOT NULL DEFAULT 0 COMMENT '总库存量',
`frozen_count`    INT NOT NULL DEFAULT 0 COMMENT '冻结库存量',
`available_count` INT GENERATED ALWAYS AS (`stock_count` - `frozen_count`) VIRTUAL COMMENT '可用库存量',
`shard_key`       TINYINT GENERATED ALWAYS AS (product_id % 8) STORED COMMENT '分片键（8分区）',
`version`         INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
`create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
`update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`id`, `shard_key`),
UNIQUE KEY `uk_product` (`product_id`, `shard_key`), -- 包含分片键的唯一约束
KEY `idx_shard` (`shard_key`)
) ENGINE = InnoDB COMMENT ='库存主表（支持高并发）'
PARTITION BY HASH (shard_key) PARTITIONS 8 -- HASH分区提升均衡性
;

-- 库存变更日志表（按年分区）
CREATE TABLE IF NOT EXISTS `stock_log`
(
`id`           BIGINT NOT NULL AUTO_INCREMENT,
`product_id`   BIGINT NOT NULL COMMENT '商品ID',
`change_type`  TINYINT NOT NULL COMMENT '类型：1=入库 2=出库 3=冻结 4=解冻',
`change_count` INT NOT NULL COMMENT '变更数量',
`before_count` INT NOT NULL COMMENT '变更前总量',
`after_count`  INT NOT NULL COMMENT '变更后总量',
`operator`     VARCHAR(50) COMMENT '操作人',
`log_year`     SMALLINT GENERATED ALWAYS AS (YEAR(create_time)) STORED COMMENT '日志年份',
`create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`, `log_year`), -- 复合主键满足分区约束
KEY `idx_product_time` (`product_id`, `create_time`)
) ENGINE = InnoDB
COMMENT ='库存变更审计表'
PARTITION BY RANGE (`log_year`) ( -- 按年分区管理
PARTITION p2025 VALUES LESS THAN (2026),
PARTITION p2026 VALUES LESS THAN (2027),
PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 入库记录表
CREATE TABLE IF NOT EXISTS `stock_in`
(
`id`          BIGINT NOT NULL AUTO_INCREMENT,
`product_id`  BIGINT NOT NULL,
`quantity`    INT NOT NULL COMMENT '入库数量',
`supplier_id` VARCHAR(50) COMMENT '供应商ID',
`batch_no`    VARCHAR(100) COMMENT '批次号',
`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
KEY `idx_product` (`product_id`)
) ENGINE = InnoDB COMMENT ='入库明细表';

-- 出库记录表
CREATE TABLE IF NOT EXISTS `stock_out`
(
`id`          BIGINT NOT NULL AUTO_INCREMENT,
`product_id`  BIGINT NOT NULL,
`quantity`    INT NOT NULL COMMENT '出库数量',
`order_id`    BIGINT COMMENT '关联订单ID',
`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
KEY `idx_order` (`order_id`)
) ENGINE = InnoDB COMMENT ='出库明细表';
-- 创建库存数据库
CREATE DATABASE IF NOT EXISTS `user_db`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;
USE `user_db`;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users
(
user_id BIGINT PRIMARY KEY COMMENT '用户ID（雪花算法）',
username VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
password_hash VARCHAR(255) NOT NULL COMMENT '加密密码',
user_type VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色类型：ADMIN/MERCHANT/USER',
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
) COMMENT ='用户表';

-- 创建商家认证信息表
CREATE TABLE IF NOT EXISTS merchant_auth
(
id BIGINT PRIMARY KEY COMMENT '认证ID',
user_id BIGINT NOT NULL COMMENT '用户ID',
shop_name VARCHAR(100) NOT NULL COMMENT '店铺名称',
business_license_number VARCHAR(100) NOT NULL COMMENT '营业执照号码',
business_license_url VARCHAR(255) NOT NULL COMMENT '营业执照图片URL',
id_card_front_url VARCHAR(255) NOT NULL COMMENT '身份证正面URL',
id_card_back_url VARCHAR(255) NOT NULL COMMENT '身份证反面URL',
contact_phone VARCHAR(20)  NOT NULL COMMENT '联系电话',
contact_address VARCHAR(255) NOT NULL COMMENT '联系地址',
status TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-审核通过，2-审核拒绝',
audit_remark VARCHAR(255) COMMENT '审核备注',
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
INDEX idx_user_id (user_id),
INDEX idx_status (status),
INDEX idx_created_at (created_at)
) COMMENT ='商家认证信息表';

-- 初始化管理员用户
INSERT INTO users (user_id, username, password_hash, user_type, email, phone, nickname, status)
VALUES (1, 'admin', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', 'ADMIN', 'admin@example.com',
'13800000000', '超级管理员', 1)
ON DUPLICATE KEY UPDATE username=username;

-- 初始化测试商家用户
INSERT INTO users (user_id, username, password_hash, user_type, email, phone, nickname, status)
VALUES (2, 'merchant', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', 'MERCHANT',
'merchant@example.com', '13800000001', '测试商家', 0)
ON DUPLICATE KEY UPDATE username=username;
-- ==================== 支付数据库 (payment_db) ====================
CREATE DATABASE IF NOT EXISTS `payment_db` DEFAULT CHARSET utf8mb4;
USE `payment_db`;

-- 支付主表（1:1订单关联）
CREATE TABLE `payment` (
`id` VARCHAR(32) PRIMARY KEY COMMENT '支付ID（雪花ID）',
`order_id` VARCHAR(32) NOT NULL UNIQUE COMMENT '订单ID',
`user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
`amount` DECIMAL(12,2) UNSIGNED NOT NULL COMMENT '支付金额',
`status` TINYINT NOT NULL COMMENT '状态：0-待支付，1-成功，2-失败，3-已退款',
`channel` TINYINT NOT NULL COMMENT '渠道：1-支付宝，2-微信，3-银行卡',
`transaction_id` VARCHAR(100) COMMENT '第三方流水号',
`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
KEY `idx_order_user` (`order_id`, `user_id`)
) ENGINE=InnoDB COMMENT='支付主表';

-- 支付流水表（资金审计）
CREATE TABLE `payment_flow` (
`id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
`payment_id` VARCHAR(32) NOT NULL COMMENT '支付ID',
`flow_type` TINYINT NOT NULL COMMENT '流水类型：1-支付，2-退款',
`amount` DECIMAL(12,2) NOT NULL COMMENT '变动金额',
`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
KEY `idx_payment_flow` (`payment_id`, `flow_type`)
) ENGINE=InnoDB COMMENT='支付流水表';
-- ==================== 订单数据库 (order_db) ====================
CREATE DATABASE IF NOT EXISTS `order_db` DEFAULT CHARSET utf8mb4;
USE `order_db`;

-- 订单主表（关键字段快照）
CREATE TABLE `order`
(
`id`               VARCHAR(32) PRIMARY KEY COMMENT '订单ID（雪花ID）',
`user_id`          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
`total_amount`     DECIMAL(12, 2) UNSIGNED NOT NULL COMMENT '订单总额',
`pay_amount`       DECIMAL(12, 2) UNSIGNED NOT NULL COMMENT '实付金额',
`status`           TINYINT NOT NULL COMMENT '状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
`address_snapshot` JSON NOT NULL COMMENT '地址快照（JSON）',
`created_at`       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
`updated_at`       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
KEY `idx_user_status` (`user_id`, `status`)
) ENGINE = InnoDB COMMENT ='订单主表';

-- 订单商品明细（防篡改设计）
CREATE TABLE `order_item`
(
`id`               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
`order_id`         VARCHAR(32)             NOT NULL COMMENT '订单ID',
`product_id`       VARCHAR(32)             NOT NULL COMMENT '商品ID',
`product_snapshot` JSON NOT NULL COMMENT '商品快照（名称/价格/规格）',
`quantity`         INT UNSIGNED NOT NULL COMMENT '购买数量',
`price`            DECIMAL(12, 2) UNSIGNED NOT NULL COMMENT '购买时单价',
KEY `idx_order_product` (`order_id`, `product_id`)
) ENGINE = InnoDB COMMENT ='订单明细表';

-- 订单操作日志（状态机追踪）
CREATE TABLE `order_operation_log`
(
`id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
`order_id`    VARCHAR(32) NOT NULL COMMENT '订单ID',
`from_status` TINYINT COMMENT '原状态',
`to_status`   TINYINT NOT NULL COMMENT '目标状态',
`operator`    VARCHAR(50) COMMENT '操作人（系统/用户ID）',
`remark`      VARCHAR(200) COMMENT '操作备注',
`created_at`  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
KEY `idx_order_status` (`order_id`, `to_status`)
) ENGINE = InnoDB COMMENT ='订单操作日志';
