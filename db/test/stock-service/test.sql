USE stock_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM stock_txn;
DELETE FROM stock_reservation;
DELETE FROM stock_ledger;

INSERT INTO stock_ledger (id, sku_id, on_hand_qty, reserved_qty, salable_qty, alert_threshold, status, deleted, version)
VALUES (60001, 51001, 500, 0, 500, 20, 1, 0, 0),
       (60002, 51002, 300, 0, 300, 20, 1, 0, 0);
