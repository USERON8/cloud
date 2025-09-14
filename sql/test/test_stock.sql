-- ==================== 库存数据库测试数据 (stock_db) ====================
USE `stock_db`;

-- 插入测试库存数据
INSERT INTO stock (id, product_id, product_name, stock_quantity, frozen_quantity, stock_status, version)
VALUES (1, 1, 'iPhone 15 Pro', 100, 0, 1, 0),
       (2, 2, 'MacBook Pro 14', 50, 0, 1, 0),
       (3, 3, '男士T恤', 200, 0, 1, 0),
       (4, 4, '女士连衣裙', 150, 0, 1, 0);

-- 插入测试入库记录数据
INSERT INTO stock_in (id, product_id, quantity)
VALUES (1, 1, 100), -- iPhone 15 Pro 入库100件
       (2, 2, 50),  -- MacBook Pro 14 入库50件
       (3, 3, 200), -- 男士T恤 入库200件
       (4, 4, 150);
-- 女士连衣裙 入库150件

-- 插入测试出库记录数据
INSERT INTO stock_out (id, product_id, order_id, quantity)
VALUES (1, 1, 1, 1), -- iPhone 15 Pro 出库1件
       (2, 3, 1, 1), -- 男士T恤 出库1件
       (3, 3, 2, 1), -- 男士T恤 出库1件
       (4, 4, 3, 1); -- 女士连衣裙 出库1件