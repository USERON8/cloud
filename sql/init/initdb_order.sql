-- ==================== 订单数据库 (order_db) ====================
DROP DATABASE IF EXISTS `order_db`;
CREATE DATABASE IF NOT EXISTS `order_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `order_db`;

-- 订单主表
CREATE TABLE `orders`
(
    id           BIGINT UNSIGNED PRIMARY KEY COMMENT '订单ID',
    user_id      BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10, 2)  NOT NULL COMMENT '订单总额',
    pay_amount   DECIMAL(10, 2)  NOT NULL COMMENT '实付金额',
    status       TINYINT         NOT NULL COMMENT '状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
    address_id   BIGINT UNSIGNED NOT NULL COMMENT '地址ID',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_user_status (user_id, status),
    INDEX idx_created_at (created_at),
    INDEX idx_status (status)
) COMMENT ='订单主表';

-- 订单明细表
CREATE TABLE `order_item`
(
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_id         BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    product_id       BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    product_snapshot JSON            NOT NULL COMMENT '商品快照',
    quantity         INT             NOT NULL COMMENT '购买数量',
    price            DECIMAL(10, 2)  NOT NULL COMMENT '购买时单价',
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_order_product (order_id, product_id),
    INDEX idx_product_id (product_id)
) COMMENT ='订单明细表';