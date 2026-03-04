USE payment_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM payment_callback_log;
DELETE FROM payment_refund;
DELETE FROM payment_order;

INSERT INTO payment_order (id, payment_no, main_order_no, sub_order_no, user_id, payment_status, payment_channel, total_amount,
                           paid_amount, transaction_no, trace_id, idempotency_key, deleted, version)
VALUES (60001, 'PAY-60001', 'M2026000001', 'S2026000001', 20001, 'CREATED', 'ALIPAY', 199.00,
        NULL, NULL, 'trace-pay-60001', 'idem-pay-60001', 0, 0);

INSERT INTO payment_refund (id, refund_payment_no, after_sale_no, payment_no, main_order_no, sub_order_no, user_id,
                            refund_status, refund_amount, refund_channel, reason, trace_id, deleted, version)
VALUES (61001, 'RFD-61001', 'AS2026000001', 'PAY-60001', 'M2026000001', 'S2026000001', 20001,
        'CREATED', 199.00, 'ALIPAY', 'quality issue', 'trace-refund-61001', 0, 0);

INSERT INTO payment_callback_log (id, callback_id, callback_type, source_channel, business_no, callback_payload, callback_status, callback_time, deleted, version)
VALUES (62001, 'CB-62001', 'PAY', 'ALIPAY', 'PAY-60001', JSON_OBJECT('trade_status', 'WAIT_BUYER_PAY'), 'NEW', NOW(), 0, 0);
