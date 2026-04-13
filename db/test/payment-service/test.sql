USE payment_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM payment_callback_log;
DELETE FROM payment_refund;
DELETE FROM payment_order;

-- index optimization
ALTER TABLE payment_order
    ADD INDEX idx_payment_order_idem_deleted (idempotency_key, deleted);

ALTER TABLE payment_order
    ADD INDEX idx_payment_order_status_deleted_next_poll (status, deleted, next_poll_at);

ALTER TABLE payment_refund
    ADD INDEX idx_payment_refund_idem_deleted (idempotency_key, deleted);

ALTER TABLE payment_refund
    ADD INDEX idx_payment_refund_status_deleted_next_retry (status, deleted, next_retry_at);

INSERT INTO payment_order (id, payment_no, main_order_no, sub_order_no, user_id, amount, provider, provider_app_id, provider_merchant_id, biz_type, biz_order_key, channel, status, idempotency_key, deleted, version)
VALUES (70001, 'PAY202603050001', 'M2026000001', 'S2026000001', 20001, 4999.00, 'ALIPAY', '2021000122671234', '2021000122671234', 'SUB_ORDER', 'M2026000001:S2026000001', 'ALIPAY', 'CREATED', 'idem-pay-70001', 0, 0),
       (70002, 'PAY202603050002', 'M2026000002', 'S2026000002', 20002, 6799.00, 'ALIPAY', '2021000122671234', '2021000122671234', 'SUB_ORDER', 'M2026000002:S2026000002', 'ALIPAY', 'PAID', 'idem-pay-70002', 0, 0),
       (70003, 'PAY202603050003', 'M2026000003', 'S2026000003', 20003, 8999.00, 'ALIPAY', '2021000122671234', '2021000122671234', 'SUB_ORDER', 'M2026000003:S2026000003', 'ALIPAY', 'PAID', 'idem-pay-70003', 0, 0),
       (70004, 'PAY202603050004', 'M2026000003', 'S2026000004', 20003, 1299.00, 'WECHAT', 'WX-APP-TEST-001', 'WX-MERCHANT-TEST-001', 'SUB_ORDER', 'M2026000003:S2026000004', 'WECHAT', 'PAID', 'idem-pay-70004', 0, 0),
       (70005, 'PAY202603050005', 'M2026000004', 'S2026000005', 20001, 5699.00, 'WECHAT', 'WX-APP-TEST-001', 'WX-MERCHANT-TEST-001', 'SUB_ORDER', 'M2026000004:S2026000005', 'WECHAT', 'PAID', 'idem-pay-70005', 0, 0),
       (70006, 'PAY202603050006', 'M2026000005', 'S2026000006', 20002, 1599.00, 'ALIPAY', '2021000122671234', '2021000122671234', 'SUB_ORDER', 'M2026000005:S2026000006', 'ALIPAY', 'PAID', 'idem-pay-70006', 0, 0),
       (70007, 'PAY202603050007', 'M2026000006', 'S2026000007', 20002, 6999.00, 'ALIPAY', '2021000122671234', '2021000122671234', 'SUB_ORDER', 'M2026000006:S2026000007', 'ALIPAY', 'PAID', 'idem-pay-70007', 0, 0);

INSERT INTO payment_callback_log (id, payment_no, provider, callback_no, callback_status, provider_event_type, provider_txn_no, verified_app_id, verified_seller_id, payload, raw_payload_hash, idempotency_key, deleted, version)
VALUES (71001, 'PAY202603050001', 'ALIPAY', 'CB202603050001', 'PENDING', 'WAIT_BUYER_PAY', 'ALI-TXN-001', '2021000122671234', '2021000122671234', '{"tradeStatus":"WAIT_BUYER_PAY"}', '4f7acfe7336bd59d626b0b3ac7df29256201c7ca2b617ab5a6bd2c76d1c7e44c', 'idem-callback-71001', 0, 0),
       (71002, 'PAY202603050002', 'ALIPAY', 'CB202603050002', 'SUCCESS', 'TRADE_SUCCESS', 'ALI-TXN-002', '2021000122671234', '2021000122671234', '{"tradeStatus":"TRADE_SUCCESS"}', '54cd1c7cf9b883de2fadef9a4c6fa7f7da0843606ec663e176cbece8412f62c6', 'idem-callback-71002', 0, 0),
       (71003, 'PAY202603050003', 'ALIPAY', 'CB202603050003', 'SUCCESS', 'TRADE_SUCCESS', 'ALI-TXN-003', '2021000122671234', '2021000122671234', '{"tradeStatus":"TRADE_SUCCESS"}', '54cd1c7cf9b883de2fadef9a4c6fa7f7da0843606ec663e176cbece8412f62c6', 'idem-callback-71003', 0, 0),
       (71004, 'PAY202603050004', 'WECHAT', 'CB202603050004', 'SUCCESS', 'SUCCESS', 'WX-TXN-004', 'WX-APP-TEST-001', 'WX-MERCHANT-TEST-001', '{"tradeState":"SUCCESS"}', '7632e558dd3d40854fb797972eb646c98ff336aec961becbebcef16e30840915', 'idem-callback-71004', 0, 0),
       (71005, 'PAY202603050005', 'WECHAT', 'CB202603050005', 'SUCCESS', 'SUCCESS', 'WX-TXN-005', 'WX-APP-TEST-001', 'WX-MERCHANT-TEST-001', '{"tradeState":"SUCCESS"}', '7632e558dd3d40854fb797972eb646c98ff336aec961becbebcef16e30840915', 'idem-callback-71005', 0, 0),
       (71006, 'PAY202603050006', 'ALIPAY', 'CB202603050006', 'SUCCESS', 'TRADE_SUCCESS', 'ALI-TXN-006', '2021000122671234', '2021000122671234', '{"tradeStatus":"TRADE_SUCCESS"}', '54cd1c7cf9b883de2fadef9a4c6fa7f7da0843606ec663e176cbece8412f62c6', 'idem-callback-71006', 0, 0),
       (71007, 'PAY202603050007', 'ALIPAY', 'CB202603050007', 'SUCCESS', 'TRADE_SUCCESS', 'ALI-TXN-007', '2021000122671234', '2021000122671234', '{"tradeStatus":"TRADE_SUCCESS"}', '54cd1c7cf9b883de2fadef9a4c6fa7f7da0843606ec663e176cbece8412f62c6', 'idem-callback-71007', 0, 0);

INSERT INTO payment_refund (id, refund_no, payment_no, provider, provider_app_id, provider_merchant_id, after_sale_no, refund_amount, status, reason, idempotency_key, deleted, version)
VALUES (72001, 'REF202603050001', 'PAY202603050003', 'ALIPAY', '2021000122671234', '2021000122671234', 'AS2026000001', 8999.00, 'REFUNDING', 'hinge feel', 'idem-refund-72001', 0, 0);
