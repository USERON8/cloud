USE payment_db;

DELETE FROM payment_flow WHERE id BETWEEN 30001 AND 30020;
DELETE FROM payment WHERE id BETWEEN 30001 AND 30020;

INSERT INTO payment (id, order_id, user_id, amount, status, channel, transaction_id, trace_id, deleted, version)
VALUES (30001, 20001, 10001, 199.00, 1, 1, 'ALI-T-30001', 'TRACE-T-30001', 0, 0),
       (30002, 20002, 10001, 299.00, 0, 2, NULL, 'TRACE-T-30002', 0, 0),
       (30003, 20003, 10002, 399.00, 1, 1, 'ALI-T-30003', 'TRACE-T-30003', 0, 0);

INSERT INTO payment_flow (id, payment_id, flow_type, amount, trace_id, deleted, version)
VALUES (30001, 30001, 1, 199.00, 'TRACE-T-30001', 0, 0),
       (30002, 30002, 1, 299.00, 'TRACE-T-30002', 0, 0),
       (30003, 30003, 1, 399.00, 'TRACE-T-30003', 0, 0);
