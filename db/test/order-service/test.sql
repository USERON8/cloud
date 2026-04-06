USE order_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM after_sale_timeline;
DELETE FROM after_sale_evidence;
DELETE FROM after_sale_item;
DELETE FROM after_sale;
DELETE FROM cart_item;
DELETE FROM cart;
DELETE FROM order_item;
DELETE FROM order_sub;
DELETE FROM order_main;

-- index optimization
ALTER TABLE order_sub
    ADD INDEX idx_order_sub_status_deleted_created (order_status, deleted, created_at);

ALTER TABLE after_sale
    ADD INDEX idx_after_sale_status_deleted_created (status, deleted, created_at);

ALTER TABLE cart_item
    ADD INDEX idx_cart_item_cart_user_selected_checked_deleted (cart_id, user_id, selected, checked_out, deleted);

INSERT INTO order_main (id, main_order_no, user_id, order_status, total_amount, payable_amount, client_order_id, idempotency_key, deleted, version)
VALUES (10001, 'M2026000001', 20001, 'CREATED', 4999.00, 4999.00, 'client-order-10001', 'idem-main-10001', 0, 0),
       (10002, 'M2026000002', 20002, 'PAID', 6999.00, 6799.00, 'client-order-10002', 'idem-main-10002', 0, 0),
       (10003, 'M2026000003', 20003, 'DONE', 10298.00, 10298.00, 'client-order-10003', 'idem-main-10003', 0, 0),
       (10004, 'M2026000004', 20001, 'DONE', 5699.00, 5699.00, 'client-order-10004', 'idem-main-10004', 0, 0),
       (10005, 'M2026000005', 20002, 'DONE', 1599.00, 1599.00, 'client-order-10005', 'idem-main-10005', 0, 0),
       (10006, 'M2026000006', 20002, 'DONE', 6999.00, 6999.00, 'client-order-10006', 'idem-main-10006', 0, 0);

INSERT INTO order_sub (id, sub_order_no, main_order_id, merchant_id, order_status, shipping_status, after_sale_status,
                       item_amount, shipping_fee, discount_amount, payable_amount, receiver_name, receiver_phone, receiver_address, deleted, version)
VALUES (11001, 'S2026000001', 10001, 30001, 'CREATED', 'PENDING', 'NONE', 4999.00, 0.00, 0.00, 4999.00,
        'Test User One', '13900000001', 'Shanghai Pudong Century Ave No.200', 0, 0),
       (11002, 'S2026000002', 10002, 30001, 'PAID', 'PENDING', 'NONE', 6999.00, 0.00, 200.00, 6799.00,
        'Test User Two', '13900000002', 'Hangzhou Yuhang Building 3', 0, 0),
       (11003, 'S2026000003', 10003, 30001, 'DONE', 'DELIVERED', 'REFUNDING', 8999.00, 0.00, 0.00, 8999.00,
        'Test User Three', '13900000003', 'Nanjing Jianye Cloud Center', 0, 0),
       (11004, 'S2026000004', 10003, 30002, 'DONE', 'DELIVERED', 'NONE', 1299.00, 0.00, 0.00, 1299.00,
        'Test User Three', '13900000003', 'Nanjing Jianye Cloud Center', 0, 0),
       (11005, 'S2026000005', 10004, 30001, 'DONE', 'DELIVERED', 'NONE', 5699.00, 0.00, 0.00, 5699.00,
        'Test User One', '13900000001', 'Shanghai Xuhui Hongqiao Rd Room 1808', 0, 0),
       (11006, 'S2026000006', 10005, 30002, 'DONE', 'DELIVERED', 'NONE', 1599.00, 0.00, 0.00, 1599.00,
        'Test User Two', '13900000002', 'Hangzhou Wenyi West Rd Building 3', 0, 0),
       (11007, 'S2026000007', 10006, 30001, 'DONE', 'DELIVERED', 'NONE', 6999.00, 0.00, 0.00, 6999.00,
        'Test User Two', '13900000002', 'Hangzhou Wenyi West Rd Building 3', 0, 0);

INSERT INTO order_item (id, main_order_id, sub_order_id, spu_id, sku_id, sku_code, sku_name, sku_snapshot, quantity, unit_price, total_price, deleted, version)
VALUES (12001, 10001, 11001, 50001, 51001, 'CP15-256-BLK', 'Cloud Phone 15 256G Black',
        JSON_OBJECT('spuName', 'Cloud Phone 15', 'spec', 'color:black;storage:256G'), 1, 4999.00, 4999.00, 0, 0),
       (12002, 10002, 11002, 50002, 51003, 'CP15P-512-GRY', 'Cloud Phone 15 Pro 512G Gray',
        JSON_OBJECT('spuName', 'Cloud Phone 15 Pro', 'spec', 'color:gray;storage:512G'), 1, 6999.00, 6999.00, 0, 0),
       (12003, 10003, 11003, 50003, 51004, 'CFX-512-BLU', 'Cloud Fold X 512G Blue',
        JSON_OBJECT('spuName', 'Cloud Fold X', 'spec', 'color:blue;storage:512G'), 1, 8999.00, 8999.00, 0, 0),
       (12004, 10003, 11004, 50004, 51006, 'CWA-STD-MNT', 'Cloud Watch Air Mint',
        JSON_OBJECT('spuName', 'Cloud Watch Air', 'spec', 'color:mint;size:42mm'), 1, 1299.00, 1299.00, 0, 0),
       (12005, 10004, 11005, 50001, 51002, 'CP15-512-SLV', 'Cloud Phone 15 512G Silver',
        JSON_OBJECT('spuName', 'Cloud Phone 15', 'spec', 'color:silver;storage:512G'), 1, 5699.00, 5699.00, 0, 0),
       (12006, 10005, 11006, 50006, 51008, 'CBM-SLV-CN', 'Cloud Brew Mini Silver',
        JSON_OBJECT('spuName', 'Cloud Brew Mini', 'spec', 'color:silver;voltage:220V'), 1, 1599.00, 1599.00, 0, 0),
       (12007, 10006, 11007, 50002, 51003, 'CP15P-512-GRY', 'Cloud Phone 15 Pro 512G Gray',
        JSON_OBJECT('spuName', 'Cloud Phone 15 Pro', 'spec', 'color:gray;storage:512G'), 1, 6999.00, 6999.00, 0, 0);

INSERT INTO cart (id, cart_no, user_id, cart_status, selected_count, total_amount, deleted, version)
VALUES (13001, 'C2026000001', 20001, 'ACTIVE', 2, 6298.00, 0, 0),
       (13002, 'C2026000002', 20002, 'ACTIVE', 1, 1599.00, 0, 0),
       (13003, 'C2026000003', 20003, 'ACTIVE', 1, 899.00, 0, 0);

INSERT INTO cart_item (id, cart_id, user_id, spu_id, sku_id, sku_name, quantity, unit_price, selected, checked_out, deleted, version)
VALUES (13101, 13001, 20001, 50001, 51001, 'Cloud Phone 15 256G Black', 1, 4999.00, 1, 0, 0, 0),
       (13102, 13001, 20001, 50004, 51006, 'Cloud Watch Air Mint', 1, 1299.00, 1, 0, 0, 0),
       (13103, 13002, 20002, 50006, 51008, 'Cloud Brew Mini Silver', 1, 1599.00, 1, 0, 0, 0),
       (13104, 13003, 20003, 50005, 51007, 'Cloud Pods Max White', 1, 899.00, 1, 0, 0, 0);

INSERT INTO after_sale (id, after_sale_no, main_order_id, sub_order_id, user_id, merchant_id, after_sale_type, status,
                        reason, description, apply_amount, approved_amount, deleted, version)
VALUES (14001, 'AS2026000001', 10003, 11003, 20003, 30001, 'REFUND', 'REFUNDING',
        'hinge feel', 'The hinge is slightly tighter than expected.', 8999.00, 8999.00, 0, 0);

INSERT INTO after_sale_item (id, after_sale_id, order_item_id, sku_id, quantity, apply_amount, approved_amount, deleted, version)
VALUES (14101, 14001, 12003, 51004, 1, 8999.00, 8999.00, 0, 0);

INSERT INTO after_sale_evidence (id, after_sale_id, evidence_type, object_key, object_url, uploaded_by, deleted, version)
VALUES (14111, 14001, 'IMAGE', 'after-sale/14001/hinge-check.jpg', 'https://img.example.com/after-sale-14001-hinge.jpg', 20003, 0, 0);

INSERT INTO after_sale_timeline (id, after_sale_id, from_status, to_status, action, operator_id, operator_role, remark, deleted, version)
VALUES (14201, 14001, NULL, 'APPLIED', 'USER_APPLY', 20003, 'USER', 'Applied after delivery.', 0, 0),
       (14202, 14001, 'APPLIED', 'APPROVED', 'MERCHANT_APPROVE', 30001, 'MERCHANT', 'Approved for refund.', 0, 0),
       (14203, 14001, 'APPROVED', 'REFUNDING', 'PROCESS', 30001, 'MERCHANT', 'Refund request has been submitted to payment.', 0, 0);
