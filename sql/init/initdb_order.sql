-- ==================== 订单数据库 (order_db) ====================
DROP DATABASE IF EXISTS `order_db`;
CREATE DATABASE IF NOT EXISTS `order_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `order_db`;

-- 订单主表
CREATE TABLE `orders`
(
    id             BIGINT UNSIGNED PRIMARY KEY COMMENT '订单ID',
    order_no       VARCHAR(32)     NOT NULL UNIQUE COMMENT '订单号（业务唯一编号）',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    total_amount   DECIMAL(10, 2)  NOT NULL COMMENT '订单总额',
    pay_amount     DECIMAL(10, 2)  NOT NULL COMMENT '实付金额',
    status         TINYINT         NOT NULL COMMENT '状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
    address_id     BIGINT UNSIGNED NOT NULL COMMENT '地址ID',
    pay_time       DATETIME        NULL COMMENT '支付时间',
    ship_time      DATETIME        NULL COMMENT '发货时间',
    complete_time  DATETIME        NULL COMMENT '完成时间',
    cancel_time    DATETIME        NULL COMMENT '取消时间',
    cancel_reason  VARCHAR(255)    NULL COMMENT '取消原因',
    remark         VARCHAR(500)    NULL COMMENT '备注',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创庺时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT UNSIGNED NULL COMMENT '创庺人',
    update_by      BIGINT UNSIGNED NULL COMMENT '更新人',
    version        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted        TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除,1-已删除',

    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_user_status (user_id, status),
    INDEX idx_create_time (create_time),
    INDEX idx_status (status),
    INDEX idx_pay_time (pay_time),
    INDEX idx_ship_time (ship_time),
    INDEX idx_complete_time (complete_time),
    INDEX idx_cancel_time (cancel_time)
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
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创庺时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT UNSIGNED NULL COMMENT '创庺人',
    update_by        BIGINT UNSIGNED NULL COMMENT '更新人',
    version          INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted          TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除,1-已删除',

    INDEX idx_order_product (order_id, product_id),
    INDEX idx_product_id (product_id),
    INDEX idx_create_time (create_time)
) COMMENT ='订单明细表';
