USE product_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM product_review;
DELETE FROM sku;
DELETE FROM spu;
DELETE FROM category;

INSERT INTO category (id, parent_id, name, level, path, sort_order, status, deleted, version)
VALUES (100, 0, 'Electronics', 1, '/100', 1, 1, 0, 0),
       (200, 100, 'Phone', 2, '/100/200', 1, 1, 0, 0),
       (300, 200, 'Smart Phone', 3, '/100/200/300', 1, 1, 0, 0);

INSERT INTO spu (id, spu_name, subtitle, category_id, brand_id, merchant_id, status, description, main_image, deleted, version)
VALUES (50001, 'Cloud Phone 15', 'Cloud flagship', 300, 7001, 9001, 1, 'Cloud Phone 15 description', 'https://img.example.com/spu-50001.jpg', 0, 0);

INSERT INTO sku (id, spu_id, sku_code, sku_name, spec_json, sale_price, market_price, cost_price, status, image_url, deleted, version)
VALUES (51001, 50001, 'CP15-256-BLK', 'Cloud Phone 15 256G Black', JSON_OBJECT('color', 'black', 'storage', '256G'), 4999.00, 5399.00, 4200.00, 1, 'https://img.example.com/sku-51001.jpg', 0, 0),
       (51002, 50001, 'CP15-512-SLV', 'Cloud Phone 15 512G Silver', JSON_OBJECT('color', 'silver', 'storage', '512G'), 5699.00, 6099.00, 4800.00, 1, 'https://img.example.com/sku-51002.jpg', 0, 0);

INSERT INTO product_review (id, spu_id, sku_id, order_sub_no, user_id, rating, content, images, tags, is_anonymous, audit_status,
                            merchant_reply, like_count, is_visible, deleted, version)
VALUES (52001, 50001, 51001, 'SUB202603050001', 30001, 5, 'very good', JSON_ARRAY('https://img.example.com/review-1.jpg'),
        'fast,smooth', 0, 'APPROVED', 'thanks', 3, 1, 0, 0);
