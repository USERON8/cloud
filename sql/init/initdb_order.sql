-- ==================== 订单数据库 (order_db) ====================
CREATE DATABASE IF NOT EXISTS `order_db` DEFAULT CHARSET utf8mb4;
USE `order_db`;

-- 订单主表（关键字段快照）
CREATE TABLE `order`
(
    `id`               VARCHAR(32) PRIMARY KEY COMMENT '订单ID（雪花ID）',
    `user_id`          BIGINT UNSIGNED         NOT NULL COMMENT '用户ID',
    `total_amount`     DECIMAL(12, 2) UNSIGNED NOT NULL COMMENT '订单总额',
    `pay_amount`       DECIMAL(12, 2) UNSIGNED NOT NULL COMMENT '实付金额',
    `status`           TINYINT                 NOT NULL COMMENT '状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
    `address_snapshot` JSON                    NOT NULL COMMENT '地址快照（JSON）',
    `created_at`       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT ='订单主表';

-- 订单商品明细（防篡改设计）
CREATE TABLE `order_item`
(
    `id`               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `order_id`         VARCHAR(32)             NOT NULL COMMENT '订单ID',
    `product_id`       VARCHAR(32)             NOT NULL COMMENT '商品ID',
    `product_snapshot` JSON                    NOT NULL COMMENT '商品快照（名称/价格/规格）',
    `quantity`         INT UNSIGNED            NOT NULL COMMENT '购买数量',
    `price`            DECIMAL(12, 2) UNSIGNED NOT NULL COMMENT '购买时单价',
    KEY `idx_order_product` (`order_id`, `product_id`)
) ENGINE = InnoDB COMMENT ='订单明细表';

-- 订单操作日志（状态机追踪）
CREATE TABLE `order_operation_log`
(
    `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `order_id`    VARCHAR(32) NOT NULL COMMENT '订单ID',
    `from_status` TINYINT COMMENT '原状态',
    `to_status`   TINYINT     NOT NULL COMMENT '目标状态',
    `operator`    VARCHAR(50) COMMENT '操作人（系统/用户ID）',
    `remark`      VARCHAR(200) COMMENT '操作备注',
    `created_at`  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_order_status` (`order_id`, `to_status`)
) ENGINE = InnoDB COMMENT ='订单操作日志';