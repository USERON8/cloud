USE stock_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM stock_txn;
DELETE FROM stock_reservation;
DELETE FROM stock_ledger;

INSERT INTO stock_ledger (id, sku_id, sku_code, sku_name, on_hand_qty, reserved_qty, salable_qty, warning_threshold, stock_status, deleted, version)
VALUES (50001, 41001, 'SKU-41001', 'Test SKU A', 100, 0, 100, 5, 'NORMAL', 0, 0);

INSERT INTO stock_reservation (id, reservation_no, main_order_no, sub_order_no, sku_id, reserved_qty, reservation_status, expire_at, deleted, version)
VALUES (51001, 'RSV-51001', 'M2026000001', 'S2026000001', 41001, 1, 'RESERVED', DATE_ADD(NOW(), INTERVAL 30 MINUTE), 0, 0);

INSERT INTO stock_txn (id, txn_no, sku_id, main_order_no, sub_order_no, txn_type, qty, before_on_hand, before_reserved, before_salable,
                       after_on_hand, after_reserved, after_salable, operator_id, remark, deleted, version)
VALUES (52001, 'TXN-52001', 41001, 'M2026000001', 'S2026000001', 'RESERVE', 1, 100, 0, 100, 100, 1, 99, 20001, 'order reserve', 0, 0);
