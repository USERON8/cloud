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

INSERT INTO order_main (id, main_order_no, user_id, order_status, total_amount, payable_amount, idempotency_key, deleted, version)
VALUES (10001, 'M2026000001', 20001, 'CREATED', 199.00, 199.00, 'idem-main-10001', 0, 0);

INSERT INTO order_sub (id, sub_order_no, main_order_id, merchant_id, order_status, shipping_status, after_sale_status,
                       item_amount, shipping_fee, discount_amount, payable_amount, receiver_name, receiver_phone, receiver_address, deleted, version)
VALUES (11001, 'S2026000001', 10001, 30001, 'CREATED', 'PENDING', 'NONE', 199.00, 0.00, 0.00, 199.00,
        'Tester', '13800000000', 'Shanghai Pudong', 0, 0);

INSERT INTO order_item (id, main_order_id, sub_order_id, spu_id, sku_id, sku_code, sku_name, sku_snapshot, quantity, unit_price, total_price, deleted, version)
VALUES (12001, 10001, 11001, 50001, 51001, 'CP15-256-BLK', 'Cloud Phone 15 256G Black',
        JSON_OBJECT('spuName', 'Cloud Phone 15', 'spec', 'color:black;storage:256G'), 1, 4999.00, 4999.00, 0, 0);

INSERT INTO cart (id, cart_no, user_id, cart_status, selected_count, total_amount, deleted, version)
VALUES (13001, 'C2026000001', 20001, 'ACTIVE', 1, 4999.00, 0, 0);

INSERT INTO cart_item (id, cart_id, user_id, spu_id, sku_id, sku_name, quantity, unit_price, selected, checked_out, deleted, version)
VALUES (13101, 13001, 20001, 50001, 51001, 'Cloud Phone 15 256G Black', 1, 4999.00, 1, 0, 0, 0);

INSERT INTO after_sale (id, after_sale_no, main_order_id, sub_order_id, user_id, merchant_id, after_sale_type, status,
                        reason, description, apply_amount, approved_amount, deleted, version)
VALUES (14001, 'AS2026000001', 10001, 11001, 20001, 30001, 'REFUND', 'APPLIED',
        'quality issue', 'box damaged', 4999.00, NULL, 0, 0);

INSERT INTO after_sale_item (id, after_sale_id, order_item_id, sku_id, quantity, apply_amount, approved_amount, deleted, version)
VALUES (14101, 14001, 12001, 51001, 1, 4999.00, NULL, 0, 0);

INSERT INTO after_sale_timeline (id, after_sale_id, from_status, to_status, action, operator_id, operator_role, remark, deleted, version)
VALUES (14201, 14001, NULL, 'APPLIED', 'USER_APPLY', 20001, 'USER', 'apply success', 0, 0);
