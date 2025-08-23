-- ==================== 支付数据库 (payment_db) ====================
CREATE DATABASE IF NOT EXISTS `payment_db` DEFAULT CHARSET utf8mb4;
USE `payment_db`;

-- 支付主表（1:1订单关联）
CREATE TABLE `payment` (
  `id` BIGINT PRIMARY KEY COMMENT '支付ID（雪花ID）',
  `order_id` BIGINT NOT NULL UNIQUE COMMENT '订单ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `amount` DECIMAL(12,2) UNSIGNED NOT NULL COMMENT '支付金额',
  `status` TINYINT NOT NULL COMMENT '状态：0-待支付，1-成功，2-失败，3-已退款',
  `channel` TINYINT NOT NULL COMMENT '渠道：1-支付宝，2-微信，3-银行卡',
  `transaction_id` VARCHAR(100) COMMENT '第三方流水号（加密存储）',
  `encrypted_transaction_id` TEXT COMMENT '加密后的第三方流水号',
  `trace_id` VARCHAR(64) COMMENT '跟踪ID，用于幂等性处理',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
  -- 优化索引以支持高并发查询
  KEY `idx_order_user` (`order_id`, `user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  -- 添加用于支付状态查询的复合索引
  KEY `idx_status_created` (`status`, `created_at`),
  -- 添加用于用户支付记录查询的复合索引
  KEY `idx_user_created` (`user_id`, `created_at`),
  -- 添加用于订单支付查询的复合索引
  KEY `idx_order_created` (`order_id`, `created_at`),
  -- 添加用于幂等性检查的索引
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB COMMENT='支付主表';

-- 支付流水表（资金审计）
CREATE TABLE `payment_flow` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `payment_id` BIGINT NOT NULL COMMENT '支付ID',
  `flow_type` TINYINT NOT NULL COMMENT '流水类型：1-支付，2-退款',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '变动金额',
  `trace_id` VARCHAR(64) COMMENT '跟踪ID，用于幂等性处理',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
  -- 优化索引以支持高并发查询
  KEY `idx_payment_flow` (`payment_id`, `flow_type`),
  -- 添加用于支付流水查询的复合索引
  KEY `idx_payment_created` (`payment_id`, `created_at`),
  -- 添加用于流水类型查询的索引
  KEY `idx_flow_type_created` (`flow_type`, `created_at`),
  -- 添加用于幂等性检查的索引
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB COMMENT='支付流水表';