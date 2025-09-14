-- ==================== 支付数据库测试数据 (payment_db) ====================
USE `payment_db`;

-- 插入测试支付数据
INSERT INTO payment (id, order_id, user_id, amount, status, channel, transaction_id, trace_id)
VALUES (1, 1, 1, 8098.00, 1, 1, 'ALIPAY20250101000001', 'TRACE000001'), -- 支付成功
       (2, 2, 1, 99.00, 1, 2, 'WECHAT20250101000002', 'TRACE000002'),   -- 支付成功
       (3, 3, 2, 299.00, 0, 1, NULL, 'TRACE000003');
-- 待支付

-- 插入测试支付流水数据
INSERT INTO payment_flow (id, payment_id, flow_type, amount, trace_id)
VALUES (1, 1, 1, 8098.00, 'TRACE000001'), -- 支付流水
       (2, 2, 1, 99.00, 'TRACE000002'),   -- 支付流水
       (3, 3, 1, 299.00, 'TRACE000003'); -- 支付流水