USE order_db;

DELETE FROM order_item WHERE id BETWEEN 20001 AND 20020;
DELETE FROM refunds WHERE id BETWEEN 20001 AND 20020;
DELETE FROM orders WHERE id BETWEEN 20001 AND 20020;

INSERT INTO orders (id, order_no, user_id, total_amount, pay_amount, status, refund_status, address_id,
                    pay_time, ship_time, complete_time, cancel_time, cancel_reason, remark, shop_id, deleted, version)
VALUES (20001, 'ORD-T-20001', 10001, 199.00, 199.00, 1, 0, 10001,
        NOW(), NULL, NULL, NULL, NULL, 'paid order', 30001, 0, 0),
       (20002, 'ORD-T-20002', 10001, 299.00, 0.00, 0, 0, 10001,
        NULL, NULL, NULL, NULL, NULL, 'pending order', 30001, 0, 0),
       (20003, 'ORD-T-20003', 10002, 399.00, 399.00, 2, 0, 10003,
        NOW(), NOW(), NULL, NULL, NULL, 'shipped order', 30002, 0, 0),
       (20004, 'ORD-T-20004', 10003, 499.00, 0.00, 4, 0, 10002,
        NULL, NULL, NULL, NOW(), 'timeout cancel', 'cancelled order', 30003, 0, 0);

INSERT INTO order_item (id, order_id, product_id, product_snapshot, quantity, price, create_by, update_by, deleted, version)
VALUES (20001, 20001, 40001, JSON_OBJECT('product_name', 'Test Product A', 'sku', 'A-1'), 1, 199.00, 10001, 10001, 0, 0),
       (20002, 20002, 40002, JSON_OBJECT('product_name', 'Test Product B', 'sku', 'B-1'), 1, 299.00, 10001, 10001, 0, 0),
       (20003, 20003, 40003, JSON_OBJECT('product_name', 'Test Product C', 'sku', 'C-1'), 1, 399.00, 10002, 10002, 0, 0);

INSERT INTO refunds (id, refund_no, order_id, order_no, user_id, merchant_id, refund_type, refund_reason,
                     refund_description, refund_amount, refund_quantity, status, audit_time, audit_remark,
                     logistics_company, logistics_no, refund_time, refund_channel, refund_transaction_no, is_deleted)
VALUES (20001, 'REF-T-20001', 20003, 'ORD-T-20003', 10002, 50001, 1, 'quality issue',
        'screen scratch', 399.00, 1, 0, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, 0);
