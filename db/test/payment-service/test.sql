USE payment_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM payment_flow;
DELETE FROM payment;

INSERT INTO payment (id, order_id, user_id, amount, status, channel, transaction_id, trace_id, deleted, version)
VALUES (60001, 12001, 20001, 199.00, 0, 1, NULL, 'trace-pay-60001', 0, 0);

INSERT INTO payment_flow (id, payment_id, flow_type, amount, trace_id, deleted, version)
VALUES (61001, 60001, 1, 199.00, 'trace-flow-61001', 0, 0);
