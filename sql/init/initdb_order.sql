-- ==================== 订单数据库 (order_db) ====================
CREATE DATABASE IF NOT EXISTS `order_db` DEFAULT CHARSET utf8mb4;
USE `order_db`;

-- 订单主表（关键字段快照）
CREATE TABLE `order`
(
    `id`           BIGINT PRIMARY KEY COMMENT '订单ID（雪花ID）',
    `user_id`      BIGINT UNSIGNED         NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(12, 2) UNSIGNED NOT NULL COMMENT '订单总额',
    `pay_amount`   DECIMAL(12, 2) UNSIGNED NOT NULL COMMENT '实付金额',
    `status`       TINYINT                 NOT NULL COMMENT '状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
    `address_id`   BIGINT UNSIGNED         NOT NULL COMMENT '地址ID',
    `created_at`   DATETIME                         DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME                         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT                 NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    -- 优化索引以支持高并发查询
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_status` (`status`),
    -- 添加用于订单状态查询的复合索引
    KEY `idx_status_created` (`status`, `created_at`),
    -- 添加用于用户订单查询的复合索引
    KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB COMMENT ='订单主表';

-- 订单商品明细（防篡改设计）
CREATE TABLE `order_item`
(
    `id`               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `order_id`         BIGINT                  NOT NULL COMMENT '订单ID',
    `product_id`       VARCHAR(32)             NOT NULL COMMENT '商品ID',
    `product_snapshot` JSON                    NOT NULL COMMENT '商品快照（名称/价格/规格）',
    `quantity`         INT UNSIGNED            NOT NULL COMMENT '购买数量',
    `price`            DECIMAL(12, 2) UNSIGNED NOT NULL COMMENT '购买时单价',
    `created_at`       DATETIME                         DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME                         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`        BIGINT COMMENT '创建人ID',
    `update_by`        BIGINT COMMENT '更新人ID',
    `deleted`          TINYINT                 NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    -- 优化索引以支持高并发查询
    KEY `idx_order_product` (`order_id`, `product_id`),
    -- 添加用于商品销售统计的索引
    KEY `idx_product_created` (`product_id`, `created_at`),
    -- 添加用于订单详情查询的索引
    KEY `idx_order_created` (`order_id`, `created_at`)
) ENGINE = InnoDB COMMENT ='订单明细表';