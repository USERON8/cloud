-- ==================== 订单数据库测试数据 (order_db) ====================
USE `order_db`;

-- 插入测试订单数据
INSERT INTO orders (id, order_no, user_id, total_amount, pay_amount, status, address_id, pay_time, ship_time, complete_time)
VALUES (1, 'ORD000001', 1, 8098.00, 8098.00, 3, 1, '2025-01-01 10:00:00', '2025-01-01 15:00:00', '2025-01-05 10:00:00'), -- 已完成订单
       (2, 'ORD000002', 1, 99.00, 99.00, 1, 1, '2025-01-02 10:00:00', NULL, NULL), -- 已支付订单
       (3, 'ORD000003', 2, 299.00, 299.00, 0, 3, NULL, NULL, NULL);
-- 待支付订单

-- 插入测试订单明细数据
INSERT INTO order_item (id, order_id, product_id, product_snapshot, quantity, price)
VALUES (1, 1, 1, '{
  "productName": "iPhone 15 Pro",
  "price": 7999.00
}', 1, 7999.00),
       (2, 1, 3, '{
         "productName": "男士T恤",
         "price": 99.00
       }', 1, 99.00),
       (3, 2, 3, '{
         "productName": "男士T恤",
         "price": 99.00
       }', 1, 99.00),
       (4, 3, 4, '{
         "productName": "女士连衣裙",
         "price": 299.00
       }', 1, 299.00);