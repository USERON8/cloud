-- ==================== 支付数据库 (payment_db) ====================
DROP DATABASE IF EXISTS `payment_db`;
CREATE DATABASE IF NOT EXISTS `payment_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `payment_db`;

-- 支付主表
CREATE TABLE `payment`
(
    id             BIGINT UNSIGNED PRIMARY KEY COMMENT '支付ID',
    order_id       BIGINT UNSIGNED NOT NULL UNIQUE COMMENT '订单ID',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    amount         DECIMAL(10, 2)  NOT NULL COMMENT '支付金额',
    status         TINYINT         NOT NULL COMMENT '状态：0-待支付，1-成功，2-失败，3-已退款',
    channel        TINYINT         NOT NULL COMMENT '渠道：1-支付宝，2-微信，3-银行卡',
    payment_method VARCHAR(20)     NOT NULL DEFAULT 'alipay' COMMENT '支付方式',
    transaction_id VARCHAR(100) COMMENT '第三方流水号',
    trace_id       VARCHAR(64)     NOT NULL COMMENT '跟踪ID',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    -- 基础索引
    INDEX idx_order_user (order_id, user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    UNIQUE INDEX uk_trace_id (trace_id),

    -- 性能优化索引
    INDEX idx_user_status (user_id, status),
    INDEX idx_method_status (payment_method, status),
    INDEX idx_user_amount (user_id, amount),
    INDEX idx_status_amount (status, amount),
    INDEX idx_method_created (payment_method, created_at)
) COMMENT ='支付主表';

-- 支付流水表
CREATE TABLE `payment_flow`
(
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    payment_id  BIGINT UNSIGNED NOT NULL COMMENT '支付ID',
    flow_type   TINYINT         NOT NULL COMMENT '流水类型：1-支付，2-退款',
    amount      DECIMAL(10, 2)  NOT NULL COMMENT '变动金额',
    trace_id    VARCHAR(64) COMMENT '跟踪ID',
    flow_status TINYINT         NOT NULL DEFAULT 1 COMMENT '流水状态：1-成功，2-失败',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    -- 基础索引
    INDEX idx_payment_flow (payment_id, flow_type),
    INDEX idx_trace_id (trace_id),

    -- 性能优化索引
    INDEX idx_payment_id (payment_id),
    INDEX idx_flow_status (flow_status, created_at)
) COMMENT ='支付流水表';