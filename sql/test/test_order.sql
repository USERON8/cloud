-- ==================== 订单数据库测试数据 (order_db) ====================
-- 版本: v2.0
-- 更新时间: 2025-01-16
-- 说明: 包含订单主表、订单明细、退款单等测试数据,涵盖超时订单场景
-- ==================================================================================
USE `order_db`;

-- 清空现有测试数据(按依赖顺序)
TRUNCATE TABLE `refund`;
TRUNCATE TABLE `order_item`;
TRUNCATE TABLE `orders`;

-- ==================== 1. 订单主表测试数据 ====================
-- 包含各种状态的订单,特别是超时未支付订单
INSERT INTO `orders` (id, order_no, user_id, total_amount, pay_amount, order_status, address_id, pay_time, ship_time, complete_time, created_at, updated_at)
VALUES
-- 已完成订单
(1, 'ORD20250111001', 10001, 8098.00, 8098.00, 'COMPLETED', 1001, '2025-01-11 10:35:00', '2025-01-11 15:00:00', '2025-01-15 10:00:00', '2025-01-11 10:30:00', '2025-01-15 10:00:00'),
(2, 'ORD20250112001', 10002, 8999.00, 8999.00, 'COMPLETED', 1002, '2025-01-12 09:10:00', '2025-01-12 14:30:00', '2025-01-16 09:00:00', '2025-01-12 09:05:00', '2025-01-16 09:00:00'),

-- 已支付订单(待发货)
(3, 'ORD20250113001', 10003, 15999.00, 15999.00, 'PAID', 1003, '2025-01-13 11:20:00', NULL, NULL, '2025-01-13 11:15:00', '2025-01-13 11:20:00'),
(4, 'ORD20250114001', 10004, 99.00, 99.00, 'PAID', 1004, '2025-01-14 10:05:00', NULL, NULL, '2025-01-14 10:00:00', '2025-01-14 10:05:00'),

-- 已发货订单(配送中)
(5, 'ORD20250113002', 10006, 1899.00, 1899.00, 'SHIPPED', 1006, '2025-01-13 16:10:00', '2025-01-14 09:30:00', NULL, '2025-01-13 16:05:00', '2025-01-14 09:30:00'),
(6, 'ORD20250114002', 10005, 99.00, 99.00, 'SHIPPED', 1005, '2025-01-14 14:20:00', '2025-01-15 08:00:00', NULL, '2025-01-14 14:15:00', '2025-01-15 08:00:00'),

-- 已取消订单
(7, 'ORD20250112002', 10007, 6999.00, 0.00, 'CANCELLED', 1007, NULL, NULL, NULL, '2025-01-12 14:00:00', '2025-01-12 14:50:00'),
(8, 'ORD20250113003', 10008, 299.00, 0.00, 'CANCELLED', 1008, NULL, NULL, NULL, '2025-01-13 09:00:00', '2025-01-13 10:30:00'),

-- 退款中订单
(9, 'ORD20250115001', 10009, 599.00, 599.00, 'REFUNDING', 1009, '2025-01-15 10:00:00', '2025-01-15 14:00:00', NULL, '2025-01-15 09:55:00', '2025-01-15 17:00:00'),

-- ==================== 超时订单测试场景 ====================
-- 这些订单创建时间较早,处于待支付状态,用于测试超时取消功能

-- 超时订单1: 创建于1小时前,已超时30分钟(假设超时时间30分钟)
(10, 'ORD20250116001', 10010, 7999.00, 0.00, 'PENDING', 1010, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 60 MINUTE), DATE_SUB(NOW(), INTERVAL 60 MINUTE)),

-- 超时订单2: 创建于2小时前,已严重超时
(11, 'ORD20250116002', 10011, 4799.00, 0.00, 'PENDING', 1011, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 120 MINUTE), DATE_SUB(NOW(), INTERVAL 120 MINUTE)),

-- 超时订单3: 创建于45分钟前,刚超时
(12, 'ORD20250116003', 10012, 1688.00, 0.00, 'PENDING', 1012, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 45 MINUTE), DATE_SUB(NOW(), INTERVAL 45 MINUTE)),

-- 超时订单4: 创建于3小时前,长时间超时
(13, 'ORD20250116004', 10013, 299.00, 0.00, 'PENDING', 1013, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 180 MINUTE), DATE_SUB(NOW(), INTERVAL 180 MINUTE)),

-- 超时订单5: 创建于50分钟前
(14, 'ORD20250116005', 10014, 1899.00, 0.00, 'PENDING', 1014, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 50 MINUTE)),

-- 正常待支付订单: 创建于10分钟前,尚未超时
(15, 'ORD20250116006', 10015, 8999.00, 0.00, 'PENDING', 1015, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 10 MINUTE)),

-- 正常待支付订单: 创建于25分钟前,接近超时但还未超时
(16, 'ORD20250116007', 10016, 99.00, 0.00, 'PENDING', 1016, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE)),

-- 正常待支付订单: 刚创建(5分钟前)
(17, 'ORD20250116008', 10017, 599.00, 0.00, 'PENDING', 1017, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE));

-- ==================== 2. 订单明细测试数据 ====================
INSERT INTO `order_item` (id, order_id, product_id, product_name, product_snapshot, quantity, price, created_at, updated_at)
VALUES
-- 订单1明细 (iPhone 15 Pro + 男士T恤)
(1, 1, 1, 'iPhone 15 Pro', '{"product_name":"iPhone 15 Pro","sku_name":"钛金属 128GB","price":7999.00,"image":"https://example.com/sku/ip15p-ti-128.jpg"}', 1, 7999.00, '2025-01-11 10:30:00', '2025-01-11 10:30:00'),
(2, 1, 5, '优衣库男士圆领T恤', '{"product_name":"优衣库男士圆领T恤","sku_name":"白色 M","price":99.00,"image":"https://example.com/sku/uni-ts-wh-m.jpg"}', 1, 99.00, '2025-01-11 10:30:00', '2025-01-11 10:30:00'),

-- 订单2明细 (iPhone 15 Pro 256GB)
(3, 2, 1, 'iPhone 15 Pro', '{"product_name":"iPhone 15 Pro","sku_name":"钛金属 256GB","price":8999.00,"image":"https://example.com/sku/ip15p-ti-256.jpg"}', 1, 8999.00, '2025-01-12 09:05:00', '2025-01-12 09:05:00'),

-- 订单3明细 (MacBook Pro 14)
(4, 3, 2, 'MacBook Pro 14', '{"product_name":"MacBook Pro 14","sku_name":"深空灰 512GB","price":15999.00,"image":"https://example.com/sku/mbp14-sg-512.jpg"}', 1, 15999.00, '2025-01-13 11:15:00', '2025-01-13 11:15:00'),

-- 订单4明细 (优衣库T恤)
(5, 4, 5, '优衣库男士圆领T恤', '{"product_name":"优衣库男士圆领T恤","sku_name":"白色 M","price":99.00}', 1, 99.00, '2025-01-14 10:00:00', '2025-01-14 10:00:00'),

-- 订单5明细 (AirPods Pro 2)
(6, 5, 7, 'AirPods Pro 2', '{"product_name":"AirPods Pro 2","price":1899.00}', 1, 1899.00, '2025-01-13 16:05:00', '2025-01-13 16:05:00'),

-- 订单6明细 (优衣库T恤)
(7, 6, 5, '优衣库男士圆领T恤', '{"product_name":"优衣库男士圆领T恤","sku_name":"黑色 M","price":99.00}', 1, 99.00, '2025-01-14 14:15:00', '2025-01-14 14:15:00'),

-- 订单7明细 (华为Mate 60 Pro) - 已取消
(8, 7, 3, '华为Mate 60 Pro', '{"product_name":"华为Mate 60 Pro","price":6999.00}', 1, 6999.00, '2025-01-12 14:00:00', '2025-01-12 14:00:00'),

-- 订单8明细 (优衣库女士连衣裙) - 已取消
(9, 8, 6, '优衣库女士连衣裙', '{"product_name":"优衣库女士连衣裙","price":299.00}', 1, 299.00, '2025-01-13 09:00:00', '2025-01-13 09:00:00'),

-- 订单9明细 (ZARA女士外套) - 退款中
(10, 9, 8, 'ZARA女士外套', '{"product_name":"ZARA女士外套","price":599.00}', 1, 599.00, '2025-01-15 09:55:00', '2025-01-15 09:55:00'),

-- 超时订单明细
(11, 10, 1, 'iPhone 15 Pro', '{"product_name":"iPhone 15 Pro","sku_name":"钛金属 128GB","price":7999.00}', 1, 7999.00, DATE_SUB(NOW(), INTERVAL 60 MINUTE), DATE_SUB(NOW(), INTERVAL 60 MINUTE)),
(12, 11, 9, 'iPad Air', '{"product_name":"iPad Air","price":4799.00}', 1, 4799.00, DATE_SUB(NOW(), INTERVAL 120 MINUTE), DATE_SUB(NOW(), INTERVAL 120 MINUTE)),
(13, 12, 10, '华为智能手表GT 4', '{"product_name":"华为智能手表GT 4","price":1688.00}', 1, 1688.00, DATE_SUB(NOW(), INTERVAL 45 MINUTE), DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(14, 13, 6, '优衣库女士连衣裙', '{"product_name":"优衣库女士连衣裙","price":299.00}', 1, 299.00, DATE_SUB(NOW(), INTERVAL 180 MINUTE), DATE_SUB(NOW(), INTERVAL 180 MINUTE)),
(15, 14, 7, 'AirPods Pro 2', '{"product_name":"AirPods Pro 2","price":1899.00}', 1, 1899.00, DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(16, 15, 1, 'iPhone 15 Pro', '{"product_name":"iPhone 15 Pro","sku_name":"钛金属 256GB","price":8999.00}', 1, 8999.00, DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(17, 16, 5, '优衣库男士圆领T恤', '{"product_name":"优衣库男士圆领T恤","sku_name":"白色 L","price":99.00}', 1, 99.00, DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(18, 17, 8, 'ZARA女士外套', '{"product_name":"ZARA女士外套","price":599.00}', 1, 599.00, DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE));

-- ==================== 3. 退款单测试数据 ====================
INSERT INTO `refund` (id, refund_no, order_id, order_no, user_id, product_id, product_name, refund_amount, refund_reason, refund_type, refund_status, apply_time, approve_time, reject_time, complete_time, refund_desc, reject_reason, created_at, updated_at)
VALUES
-- 已完成的退款
(1, 'REF20250115001', 9, 'ORD20250115001', 10009, 8, 'ZARA女士外套', 599.00, '质量问题', 'REFUND_ONLY', 'COMPLETED', '2025-01-15 17:00:00', '2025-01-15 18:00:00', NULL, '2025-01-15 19:00:00', '商品质量不符合描述,申请退款', NULL, '2025-01-15 17:00:00', '2025-01-15 19:00:00'),

-- 待审核的退款申请
(2, 'REF20250116001', 5, 'ORD20250113002', 10006, 7, 'AirPods Pro 2', 1899.00, '不想要了', 'RETURN_REFUND', 'PENDING', '2025-01-16 10:00:00', NULL, NULL, NULL, '买错了,想退货退款', NULL, '2025-01-16 10:00:00', '2025-01-16 10:00:00'),

-- 已拒绝的退款
(3, 'REF20250115002', 2, 'ORD20250112001', 10002, 1, 'iPhone 15 Pro', 8999.00, '不喜欢', 'RETURN_REFUND', 'REJECTED', '2025-01-15 10:00:00', NULL, '2025-01-15 11:00:00', NULL, '颜色不喜欢,想退货', '商品已拆封激活,不支持退货', '2025-01-15 10:00:00', '2025-01-15 11:00:00');

COMMIT;

-- ==================== 测试数据说明 ====================
--
-- 1. 订单状态分布:
--    - PENDING(待支付): 订单10-17 (8个订单)
--      * 超时订单(5个): 订单10,11,12,13,14 (创建时间 > 30分钟前)
--      * 正常待支付(3个): 订单15,16,17 (创建时间 <= 30分钟前)
--    - PAID(已支付): 订单3,4 (2个订单)
--    - SHIPPED(已发货): 订单5,6 (2个订单)
--    - COMPLETED(已完成): 订单1,2 (2个订单)
--    - CANCELLED(已取消): 订单7,8 (2个订单)
--    - REFUNDING(退款中): 订单9 (1个订单)
--
-- 2. 超时订单详细说明(假设超时阈值30分钟):
--    - 订单10: 创建于60分钟前,超时30分钟,金额7999.00
--    - 订单11: 创建于120分钟前,超时90分钟,金额4799.00 (严重超时)
--    - 订单12: 创建于45分钟前,超时15分钟,金额1688.00
--    - 订单13: 创建于180分钟前,超时150分钟,金额299.00 (严重超时)
--    - 订单14: 创建于50分钟前,超时20分钟,金额1899.00
--
-- 3. 待支付但未超时订单:
--    - 订单15: 创建于10分钟前,还有20分钟支付时间,金额8999.00
--    - 订单16: 创建于25分钟前,还有5分钟支付时间,金额99.00 (接近超时)
--    - 订单17: 创建于5分钟前,还有25分钟支付时间,金额599.00
--
-- 4. 退款单状态:
--    - COMPLETED(已完成): 退款1 - ZARA女士外套退款599元
--    - PENDING(待审核): 退款2 - AirPods Pro 2退货退款1899元
--    - REJECTED(已拒绝): 退款3 - iPhone 15 Pro退款被拒绝(已激活)
--
-- 5. 测试场景覆盖:
--    - 订单超时自动取消功能(5个超时订单)
--    - 订单超时预警功能(订单16接近超时)
--    - 订单正常支付流程
--    - 订单完整生命周期(待支付->已支付->已发货->已完成)
--    - 订单取消流程
--    - 订单退款流程(仅退款、退货退款)
--    - 退款审核流程(通过、拒绝)
--    - 订单导出功能(各种状态订单)
--
-- 6. 定时任务测试建议:
--    - 启动 OrderTimeoutScheduledTask 定时任务
--    - 观察超时订单(10,11,12,13,14)是否被自动取消
--    - 检查库存是否正确释放
--    - 验证超时订单日报生成
--    - 测试手动调整超时时间配置
--
-- 7. 订单导出测试数据:
--    - 按状态导出: 各种状态都有订单数据
--    - 按时间范围导出: 订单创建时间跨度大
--    - 按用户导出: 用户10001-10017都有订单
--    - 按金额范围导出: 金额从99元到15999元不等
--
