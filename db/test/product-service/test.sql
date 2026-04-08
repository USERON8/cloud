USE product_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM product_review;
DELETE FROM sku;
DELETE FROM spu;
DELETE FROM brand_authorization;
DELETE FROM brand;
DELETE FROM category;

-- index optimization
ALTER TABLE category
    ADD INDEX idx_category_parent_deleted_sort (parent_id, deleted, sort_order);

ALTER TABLE category
    ADD INDEX idx_category_level_deleted_sort (level, deleted, sort_order);

ALTER TABLE category
    ADD INDEX idx_category_status_deleted_sort (status, deleted, sort_order);

INSERT INTO category (id, parent_id, name, level, path, sort_order, status, deleted, version)
VALUES (100, 0, 'Electronics', 1, '/100', 1, 1, 0, 0),
       (110, 0, 'Home Living', 1, '/110', 2, 1, 0, 0),
       (200, 100, 'Phone', 2, '/100/200', 1, 1, 0, 0),
       (210, 100, 'Wearable', 2, '/100/210', 2, 1, 0, 0),
       (220, 100, 'Audio', 2, '/100/220', 3, 1, 0, 0),
       (300, 200, 'Smart Phone', 3, '/100/200/300', 1, 1, 0, 0),
       (310, 200, 'Foldable Phone', 3, '/100/200/310', 2, 1, 0, 0),
       (320, 210, 'Smart Watch', 3, '/100/210/320', 1, 1, 0, 0),
       (330, 220, 'Wireless Earbuds', 3, '/100/220/330', 1, 1, 0, 0),
       (400, 110, 'Kitchen Appliance', 2, '/110/400', 1, 1, 0, 0),
       (410, 400, 'Coffee Appliance', 3, '/110/400/410', 1, 1, 0, 0);

INSERT INTO brand (id, brand_name, brand_name_en, logo_url, description, brand_story, official_website, country, founded_year, status,
                   is_hot, is_recommended, product_count, sort_order, seo_keywords, seo_description, deleted, version)
VALUES (7001, 'Cloud Mobile', 'Cloud Mobile', 'https://img.example.com/brand-7001.png', 'Cloud Mobile flagship handset brand.',
        NULL, 'https://mobile.example.com', 'China', 2015, 1, 1, 1, 2, 1, 'cloud phone,smart phone', 'Cloud Mobile phones and flagship devices.', 0, 0),
       (7002, 'Cloud Fold', 'Cloud Fold', 'https://img.example.com/brand-7002.png', 'Foldable devices built for productivity.',
        NULL, 'https://fold.example.com', 'China', 2019, 1, 1, 1, 1, 1, 'foldable phone,large screen phone', 'Cloud Fold premium foldable devices.', 0, 0),
       (7003, 'Cloud Wear', 'Cloud Wear', 'https://img.example.com/brand-7003.png', 'Wearable devices for sports and wellness.',
        NULL, 'https://wear.example.com', 'China', 2018, 1, 1, 1, 1, 2, 'smart watch,fitness watch', 'Cloud Wear smart watches and wearables.', 0, 0),
       (7004, 'Cloud Audio', 'Cloud Audio', 'https://img.example.com/brand-7004.png', 'Wireless audio devices for daily listening.',
        NULL, 'https://audio.example.com', 'China', 2017, 1, 0, 1, 1, 3, 'wireless earbuds,audio', 'Cloud Audio wireless earbuds and accessories.', 0, 0),
       (7005, 'Cloud Home', 'Cloud Home', 'https://img.example.com/brand-7005.png', 'Compact home appliances for modern kitchens.',
        NULL, 'https://home.example.com', 'China', 2016, 1, 0, 1, 1, 4, 'coffee machine,home appliance', 'Cloud Home compact appliances for daily use.', 0, 0);

INSERT INTO brand_authorization (id, brand_id, brand_name, merchant_id, merchant_name, auth_type, auth_status, certificate_url,
                                 start_time, end_time, deleted, version)
VALUES (73001, 7001, 'Cloud Mobile', 30001, 'Cloud Devices Flagship', 'EXCLUSIVE', 'APPROVED',
        'https://img.example.com/auth-73001.png', '2025-01-01 00:00:00', '2027-12-31 23:59:59', 0, 0),
       (73002, 7002, 'Cloud Fold', 30001, 'Cloud Devices Flagship', 'EXCLUSIVE', 'APPROVED',
        'https://img.example.com/auth-73002.png', '2025-01-01 00:00:00', '2027-12-31 23:59:59', 0, 0),
       (73003, 7003, 'Cloud Wear', 30002, 'Cloud Life Store', 'STANDARD', 'APPROVED',
        'https://img.example.com/auth-73003.png', '2025-01-01 00:00:00', '2027-12-31 23:59:59', 0, 0),
       (73004, 7004, 'Cloud Audio', 30002, 'Cloud Life Store', 'STANDARD', 'APPROVED',
        'https://img.example.com/auth-73004.png', '2025-01-01 00:00:00', '2027-12-31 23:59:59', 0, 0),
       (73005, 7005, 'Cloud Home', 30002, 'Cloud Life Store', 'STANDARD', 'APPROVED',
        'https://img.example.com/auth-73005.png', '2025-01-01 00:00:00', '2027-12-31 23:59:59', 0, 0);

INSERT INTO spu (id, spu_name, subtitle, category_id, brand_id, merchant_id, status, description, main_image, deleted, version)
VALUES (50001, 'Cloud Phone 15', 'Balanced flagship for daily use', 300, 7001, 30001, 1, 'Cloud Phone 15 description', NULL, 0, 0),
       (50002, 'Cloud Phone 15 Pro', 'Premium camera focused flagship', 300, 7001, 30001, 1, 'Cloud Phone 15 Pro description', 'https://img.example.com/spu-50002.jpg', 0, 0),
       (50003, 'Cloud Fold X', 'Large screen foldable phone', 310, 7002, 30001, 1, 'Cloud Fold X description', NULL, 0, 0),
       (50004, 'Cloud Watch Air', 'Lightweight fitness watch', 320, 7003, 30002, 1, 'Cloud Watch Air description', NULL, 0, 0),
       (50005, 'Cloud Pods Max', 'Noise cancelling wireless earbuds', 330, 7004, 30002, 1, 'Cloud Pods Max description', 'https://img.example.com/spu-50005.jpg', 0, 0),
       (50006, 'Cloud Brew Mini', 'Compact capsule coffee maker', 410, 7005, 30002, 1, 'Cloud Brew Mini description', NULL, 0, 0);

INSERT INTO sku (id, spu_id, sku_code, sku_name, spec_json, sale_price, market_price, cost_price, status, image_url, deleted, version)
VALUES (51001, 50001, 'CP15-256-BLK', 'Cloud Phone 15 256G Black', JSON_OBJECT('color', 'black', 'storage', '256G'), 4999.00, 5399.00, 4200.00, 1, NULL, 0, 0),
       (51002, 50001, 'CP15-512-SLV', 'Cloud Phone 15 512G Silver', JSON_OBJECT('color', 'silver', 'storage', '512G'), 5699.00, 6099.00, 4800.00, 1, 'https://img.example.com/sku-51002.jpg', 0, 0),
       (51003, 50002, 'CP15P-512-GRY', 'Cloud Phone 15 Pro 512G Gray', JSON_OBJECT('color', 'gray', 'storage', '512G'), 6999.00, 7599.00, 5900.00, 1, 'https://img.example.com/sku-51003.jpg', 0, 0),
       (51004, 50003, 'CFX-512-BLU', 'Cloud Fold X 512G Blue', JSON_OBJECT('color', 'blue', 'storage', '512G'), 8999.00, 9599.00, 7600.00, 1, NULL, 0, 0),
       (51005, 50003, 'CFX-1TB-BLK', 'Cloud Fold X 1T Black', JSON_OBJECT('color', 'black', 'storage', '1T'), 9999.00, 10699.00, 8400.00, 1, NULL, 0, 0),
       (51006, 50004, 'CWA-STD-MNT', 'Cloud Watch Air Mint', JSON_OBJECT('color', 'mint', 'size', '42mm'), 1299.00, 1499.00, 860.00, 1, NULL, 0, 0),
       (51007, 50005, 'CPM-WHT-STD', 'Cloud Pods Max White', JSON_OBJECT('color', 'white', 'edition', 'standard'), 899.00, 1099.00, 540.00, 1, 'https://img.example.com/sku-51007.jpg', 0, 0),
       (51008, 50006, 'CBM-SLV-CN', 'Cloud Brew Mini Silver', JSON_OBJECT('color', 'silver', 'voltage', '220V'), 1599.00, 1799.00, 980.00, 1, NULL, 0, 0);

INSERT INTO product_review (id, spu_id, sku_id, order_sub_no, user_id, rating, content, images, tags, is_anonymous, audit_status,
                            merchant_reply, like_count, is_visible, deleted, version)
VALUES (52001, 50001, 51002, 'S2026000005', 20001, 5, 'Smooth system and stable battery.', JSON_ARRAY(), 'fast,smooth', 0, 'APPROVED', 'Thanks for your feedback.', 8, 1, 0, 0),
       (52002, 50002, 51003, 'S2026000007', 20002, 5, 'The camera and screen are both excellent.', JSON_ARRAY('https://img.example.com/review-52002.jpg'), 'camera,display', 0, 'APPROVED', 'Appreciate your review.', 5, 1, 0, 0),
       (52003, 50003, 51004, 'S2026000003', 20003, 4, 'Large inner screen is useful for work.', JSON_ARRAY(), 'productivity,foldable', 1, 'APPROVED', 'We will keep improving the hinge feel.', 2, 1, 0, 0),
       (52004, 50006, 51008, 'S2026000006', 20002, 4, 'Compact machine and easy to clean.', JSON_ARRAY(), 'kitchen,coffee', 0, 'APPROVED', 'Happy brewing.', 3, 1, 0, 0),
       (52005, 50004, 51006, 'S2026000004', 20003, 4, 'Light to wear and accurate for running.', JSON_ARRAY(), 'wearable,fitness', 0, 'APPROVED', 'Thanks for sharing your fitness experience.', 4, 1, 0, 0);
