USE product_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM product_review;
DELETE FROM sku_price_history;
DELETE FROM sku;
DELETE FROM spu;
DELETE FROM category;

INSERT INTO category (id, parent_id, category_name, level, category_path, sort_order, status, deleted, version)
VALUES (100, 0, 'Electronics', 1, '100', 1, 1, 0, 0),
       (200, 100, 'Phone', 2, '100/200', 1, 1, 0, 0),
       (300, 200, 'Smart Phone', 3, '100/200/300', 1, 1, 0, 0);

INSERT INTO spu (id, spu_no, merchant_id, category_id, title, sub_title, brand_name, sale_status, audit_status, deleted, version)
VALUES (40001, 'SPU-40001', 30001, 300, 'Test SPU A', 'Sub title A', 'CloudBrand', 'ON_SHELF', 'APPROVED', 0, 0);

INSERT INTO sku (id, sku_no, spu_id, merchant_id, category_id, sku_code, sku_name, specs_json, sale_price, original_price,
                 cost_price, status, sales_count, deleted, version)
VALUES (41001, 'SKU-41001', 40001, 30001, 300, 'SKU-41001', 'Test SKU A',
        JSON_OBJECT('color', 'black', 'memory', '256G'),
        199.00, 299.00, 129.00, 'ON_SHELF', 0, 0, 0);

INSERT INTO sku_price_history (id, sku_id, old_price, new_price, changed_by, change_reason, deleted, version)
VALUES (42001, 41001, 299.00, 199.00, 30001, 'promotion', 0, 0);

INSERT INTO product_review (id, spu_id, sku_id, order_item_id, order_sub_no, user_id, rating, review_content, review_status, deleted, version)
VALUES (43001, 40001, 41001, 12001, 'S2026000001', 20001, 5, 'great', 'APPROVED', 0, 0);
