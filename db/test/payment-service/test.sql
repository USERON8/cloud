USE payment_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM payment_callback_log;
DELETE FROM payment_refund;
DELETE FROM payment_order;

INSERT INTO payment_order (id, payment_no, main_order_no, sub_order_no, user_id, amount, channel, status, idempotency_key, deleted, version)
VALUES (70001, 'PAY202603050001', 'M2026000001', 'S2026000001', 20001, 4999.00, 'ALIPAY', 'CREATED', 'idem-pay-70001', 0, 0);

INSERT INTO payment_callback_log (id, payment_no, callback_no, callback_status, provider_txn_no, payload, idempotency_key, deleted, version)
VALUES (71001, 'PAY202603050001', 'CB202603050001', 'SUCCESS', 'ALI-TXN-001', '{"tradeStatus":"TRADE_SUCCESS"}', 'idem-callback-71001', 0, 0);

INSERT INTO payment_refund (id, refund_no, payment_no, after_sale_no, refund_amount, status, reason, idempotency_key, deleted, version)
VALUES (72001, 'REF202603050001', 'PAY202603050001', 'AS2026000001', 4999.00, 'REFUNDED', 'quality issue', 'idem-refund-72001', 0, 0);
