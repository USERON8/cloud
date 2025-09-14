-- ==================== 商品数据库测试数据 (product_db) ====================
USE `product_db`;

-- 插入测试商家店铺数据
INSERT INTO merchant_shop (id, merchant_id, shop_name, avatar_url, description, contact_phone, address, status)
VALUES (1, 1, '测试店铺1', 'https://example.com/shop1.jpg', '这是测试店铺1的描述信息', '13800000003',
        '北京市朝阳区某某街道1001号', 1),
       (2, 2, '测试店铺2', 'https://example.com/shop2.jpg', '这是测试店铺2的描述信息', '13800000006',
        '上海市浦东新区某某街道2002号', 1);

-- 插入测试商品分类数据
INSERT INTO category (id, parent_id, name, level, status)
VALUES (1, 0, '电子产品', 1, 1),
       (2, 0, '服装', 1, 1),
       (3, 1, '手机', 2, 1),
       (4, 1, '电脑', 2, 1),
       (5, 2, '男装', 2, 1),
       (6, 2, '女装', 2, 1);

-- 插入测试商品数据
INSERT INTO products (id, shop_id, product_name, price, stock_quantity, category_id, status)
VALUES (1, 1, 'iPhone 15 Pro', 7999.00, 100, 3, 1),
       (2, 1, 'MacBook Pro 14', 15999.00, 50, 4, 1),
       (3, 2, '男士T恤', 99.00, 200, 5, 1),
       (4, 2, '女士连衣裙', 299.00, 150, 6, 1);