USE product_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM merchant_shop;
DELETE FROM attribute_template;
DELETE FROM brand_authorization;
DELETE FROM brand;
DELETE FROM sku_specification;
DELETE FROM product_audit;
DELETE FROM product_attribute;
DELETE FROM product_review;
DELETE FROM product_sku;
DELETE FROM products;
DELETE FROM category;

INSERT INTO category (id, parent_id, name, level, sort_order, status, create_by, update_by, deleted, version)
VALUES (100, 0, 'Electronics', 1, 1, 1, 1, 1, 0, 0),
       (200, 100, 'Phone', 2, 1, 1, 1, 1, 0, 0),
       (300, 200, 'Smart Phone', 3, 1, 1, 1, 1, 0, 0);

INSERT INTO brand (id, brand_name, status, is_hot, is_recommended, product_count, sort_order, deleted, version)
VALUES (20001, 'CloudBrand', 1, 0, 0, 1, 1, 0, 0);

INSERT INTO merchant_shop (id, merchant_id, shop_name, status, deleted, version)
VALUES (30001, 30001, 'Cloud Shop', 1, 0, 0);

INSERT INTO products (id, product_name, price, category_id, brand_id, status, stock_quantity, shop_id, deleted, version)
VALUES (40001, 'Test Product A', 199.00, 300, 20001, 1, 100, 30001, 0, 0);

INSERT INTO product_sku (id, product_id, sku_code, sku_name, spec_values, price, original_price, cost_price, stock_quantity,
                         sales_quantity, status, sort_order, deleted, version)
VALUES (41001, 40001, 'SKU-41001', 'Test SKU A', 'color:black;memory:256G',
        199.00, 299.00, 129.00, 100, 0, 1, 1, 0, 0);

INSERT INTO product_review (id, product_id, product_name, sku_id, order_id, order_no, user_id, user_nickname, rating,
                            content, is_anonymous, audit_status, like_count, is_visible, review_type, deleted, version)
VALUES (43001, 40001, 'Test Product A', 41001, 12001, 'ORD-12001', 20001, 'demo-user',
        5, 'great', 0, 'APPROVED', 0, 1, 'NORMAL', 0, 0);
