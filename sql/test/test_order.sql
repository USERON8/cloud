-- ==================== 订单数据库测试数据 (order_db) ====================
USE `order_db`;

-- 插入测试订单数据
INSERT INTO orders (id, user_id, total_amount, pay_amount, status, address_id)
VALUES (1, 1, 8098.00, 8098.00, 3, 1), -- 已完成订单
       (2, 1, 99.00, 99.00, 1, 1),     -- 已支付订单
       (3, 2, 299.00, 299.00, 0, 3);
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