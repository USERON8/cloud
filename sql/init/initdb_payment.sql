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
  KEY `idx_order_user` (`order_id`, `user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
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