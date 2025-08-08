-- 创建库存数据库
CREATE DATABASE IF NOT EXISTS `stock_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `stock_db`;

-- 产品表（基础信息）
CREATE TABLE IF NOT EXISTS `products`
(
    `product_id`  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '商品ID（主键）',
    `name`        VARCHAR(100) NOT NULL COMMENT '商品名称',
    `description` TEXT COMMENT '商品描述',
    `category_id` INT      DEFAULT 0 COMMENT '分类ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`product_id`),
    KEY `idx_category` (`category_id`)
) ENGINE = InnoDB COMMENT ='商品基础信息表';

-- 库存主表（动态计算可用库存）
CREATE TABLE IF NOT EXISTS `stock`
(
    `id`              BIGINT NOT NULL AUTO_INCREMENT,
    `product_id`      BIGINT NOT NULL COMMENT '商品ID',
    `stock_count`     INT    NOT NULL DEFAULT 0 COMMENT '总库存量',
    `frozen_count`    INT    NOT NULL DEFAULT 0 COMMENT '冻结库存量',
    `available_count` INT GENERATED ALWAYS AS (`stock_count` - `frozen_count`) VIRTUAL COMMENT '可用库存量',
    `shard_key`       TINYINT GENERATED ALWAYS AS (product_id % 8) STORED COMMENT '分片键（8分区）',
    `version`         INT    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`, `shard_key`),
    UNIQUE KEY `uk_product` (`product_id`, `shard_key`), -- 包含分片键的唯一约束
    KEY `idx_shard` (`shard_key`)
) ENGINE = InnoDB COMMENT ='库存主表（支持高并发）'
    PARTITION BY HASH (shard_key) PARTITIONS 8 -- HASH分区提升均衡性
;

-- 库存变更日志表（按年分区）
CREATE TABLE IF NOT EXISTS `stock_log`
(
    `id`           BIGINT  NOT NULL AUTO_INCREMENT,
    `product_id`   BIGINT  NOT NULL COMMENT '商品ID',
    `change_type`  TINYINT NOT NULL COMMENT '类型：1=入库 2=出库 3=冻结 4=解冻',
    `change_count` INT     NOT NULL COMMENT '变更数量',
    `before_count` INT     NOT NULL COMMENT '变更前总量',
    `after_count`  INT     NOT NULL COMMENT '变更后总量',
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
    `quantity`    INT    NOT NULL COMMENT '入库数量',
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
    `quantity`    INT    NOT NULL COMMENT '出库数量',
    `order_id`    BIGINT COMMENT '关联订单ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE = InnoDB COMMENT ='出库明细表';
