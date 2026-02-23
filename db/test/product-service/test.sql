USE product_db;

DELETE FROM product_attribute WHERE id BETWEEN 40001 AND 40050;
DELETE FROM sku_specification WHERE id BETWEEN 40001 AND 40050;
DELETE FROM product_sku WHERE id BETWEEN 40001 AND 40050;
DELETE FROM product_review WHERE id BETWEEN 40001 AND 40050;
DELETE FROM product_audit WHERE id BETWEEN 40001 AND 40050;
DELETE FROM attribute_template WHERE id BETWEEN 40001 AND 40050;
DELETE FROM brand_authorization WHERE id BETWEEN 40001 AND 40050;
DELETE FROM brand WHERE id BETWEEN 40001 AND 40050;
DELETE FROM products WHERE id BETWEEN 40001 AND 40050;
DELETE FROM merchant_shop WHERE id BETWEEN 30001 AND 30050;
DELETE FROM category WHERE id BETWEEN 30001 AND 30050;

INSERT INTO category (id, parent_id, name, level, sort_order, status, create_by, update_by, deleted, version)
VALUES (30001, 0, 'Electronics', 1, 1, 1, 1, 1, 0, 0),
       (30002, 30001, 'Smartphone', 2, 1, 1, 1, 1, 0, 0),
       (30003, 30001, 'Laptop', 2, 2, 1, 1, 1, 0, 0);

INSERT INTO merchant_shop (id, merchant_id, shop_name, avatar_url, description, contact_phone, address, status, deleted, version)
VALUES (30001, 10001, 'Test Digital Shop A', 'https://example.com/shop-a.png', 'official store', '13910000001', 'Beijing A', 1, 0, 0),
       (30002, 10002, 'Test Digital Shop B', 'https://example.com/shop-b.png', 'authorized store', '13910000002', 'Shanghai B', 1, 0, 0);

INSERT INTO brand (id, brand_name, brand_name_en, logo_url, description, country, founded_year, status, is_hot, is_recommended,
                   product_count, sort_order, deleted, version)
VALUES (40001, 'TestBrandA', 'TestBrandA', 'https://example.com/brand-a.png', 'brand a', 'CN', 2010, 1, 1, 1, 2, 1, 0, 0),
       (40002, 'TestBrandB', 'TestBrandB', 'https://example.com/brand-b.png', 'brand b', 'US', 2000, 1, 0, 1, 1, 2, 0, 0);

INSERT INTO products (id, shop_id, product_name, price, stock_quantity, category_id, status, deleted, version)
VALUES (40001, 30001, 'Test Phone A', 1999.00, 100, 30002, 1, 0, 0),
       (40002, 30001, 'Test Laptop A', 5999.00, 50, 30003, 1, 0, 0),
       (40003, 30002, 'Test Phone B', 2999.00, 80, 30002, 1, 0, 0);

INSERT INTO brand_authorization (id, brand_id, brand_name, merchant_id, merchant_name, auth_type, auth_status,
                                 certificate_url, start_time, end_time, created_at, updated_at, deleted, version)
VALUES (40001, 40001, 'TestBrandA', 10001, 'Merchant A', 'OFFICIAL', 'APPROVED',
        'https://example.com/cert-a.pdf', NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), NOW(), NOW(), 0, 0),
       (40002, 40002, 'TestBrandB', 10002, 'Merchant B', 'AUTHORIZED', 'PENDING',
        'https://example.com/cert-b.pdf', NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), NOW(), NOW(), 0, 0);

INSERT INTO attribute_template (id, template_name, category_id, attributes, description, status, is_system, usage_count, creator_id, deleted, version)
VALUES (40001, 'Phone Template', 30002, '[{\"name\":\"color\"},{\"name\":\"storage\"}]', 'phone attrs', 1, 1, 1, 1, 0, 0);

INSERT INTO product_audit (id, product_id, product_name, merchant_id, merchant_name, audit_status, audit_type, submit_time,
                           auditor_id, auditor_name, audit_time, audit_comment, priority, deleted, version)
VALUES (40001, 40001, 'Test Phone A', 10001, 'Merchant A', 'APPROVED', 'CREATE', NOW(),
        1, 'Auditor', NOW(), 'ok', 2, 0, 0);

INSERT INTO product_review (id, product_id, product_name, sku_id, order_id, order_no, user_id, user_nickname, rating, content,
                            is_anonymous, audit_status, like_count, is_visible, review_type, deleted, version)
VALUES (40001, 40001, 'Test Phone A', NULL, 20001, 'ORD-T-20001', 10001, 'user1', 5, 'great', 0, 'APPROVED', 10, 1, 'INITIAL', 0, 0);

INSERT INTO product_sku (id, product_id, sku_code, sku_name, spec_values, price, original_price, cost_price, stock_quantity,
                         sales_quantity, image_url, weight, volume, barcode, status, sort_order, deleted, version)
VALUES (40001, 40001, 'TPA-128-BLK', 'Test Phone A 128 Black', '{\"color\":\"black\",\"storage\":\"128\"}', 1999.00, 2199.00,
        1500.00, 60, 5, 'https://example.com/tpa-128.png', 180, 1, '690000000001', 1, 1, 0, 0),
       (40002, 40001, 'TPA-256-BLK', 'Test Phone A 256 Black', '{\"color\":\"black\",\"storage\":\"256\"}', 2299.00, 2499.00,
        1700.00, 40, 3, 'https://example.com/tpa-256.png', 180, 1, '690000000002', 1, 2, 0, 0);

INSERT INTO sku_specification (id, spec_name, spec_values, category_id, spec_type, is_required, sort_order, status, description, deleted, version)
VALUES (40001, 'color', '[\"black\",\"white\"]', 30002, 1, 1, 1, 1, 'color spec', 0, 0),
       (40002, 'storage', '[\"128\",\"256\"]', 30002, 1, 1, 2, 1, 'storage spec', 0, 0);

INSERT INTO product_attribute (id, product_id, attr_name, attr_value, attr_group, attr_type, is_filterable, is_list_visible,
                               is_detail_visible, sort_order, unit, deleted, version)
VALUES (40001, 40001, 'screen', '6.7', 'basic', 2, 1, 1, 1, 1, 'inch', 0, 0),
       (40002, 40001, 'battery', '5000', 'basic', 2, 1, 1, 1, 2, 'mAh', 0, 0);
