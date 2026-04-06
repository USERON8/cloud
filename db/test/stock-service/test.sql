USE stock_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM stock_txn;
DELETE FROM stock_reservation;
DELETE FROM stock_segment;

INSERT INTO stock_segment (
    id, sku_id, segment_id, available_qty, locked_qty, sold_qty, alert_threshold, status, deleted, version
)
VALUES (600011, 51001, 0, 125, 0, 16, 20, 1, 0, 0),
       (600012, 51001, 1, 125, 0, 14, 20, 1, 0, 0),
       (600013, 51001, 2, 125, 0, 10, 20, 1, 0, 0),
       (600014, 51001, 3, 125, 0, 12, 20, 1, 0, 0),
       (600021, 51002, 0, 75, 0, 8, 20, 1, 0, 0),
       (600022, 51002, 1, 75, 0, 7, 20, 1, 0, 0),
       (600023, 51002, 2, 75, 0, 6, 20, 1, 0, 0),
       (600024, 51002, 3, 75, 0, 5, 20, 1, 0, 0),
       (600031, 51003, 0, 48, 0, 22, 10, 1, 0, 0),
       (600032, 51003, 1, 48, 0, 24, 10, 1, 0, 0),
       (600033, 51003, 2, 48, 0, 19, 10, 1, 0, 0),
       (600034, 51003, 3, 48, 0, 21, 10, 1, 0, 0),
       (600041, 51004, 0, 20, 0, 11, 6, 1, 0, 0),
       (600042, 51004, 1, 20, 0, 9, 6, 1, 0, 0),
       (600043, 51004, 2, 20, 0, 8, 6, 1, 0, 0),
       (600044, 51004, 3, 20, 0, 10, 6, 1, 0, 0),
       (600051, 51005, 0, 12, 0, 4, 4, 1, 0, 0),
       (600052, 51005, 1, 12, 0, 4, 4, 1, 0, 0),
       (600053, 51005, 2, 12, 0, 5, 4, 1, 0, 0),
       (600054, 51005, 3, 12, 0, 3, 4, 1, 0, 0),
       (600061, 51006, 0, 90, 0, 30, 15, 1, 0, 0),
       (600062, 51006, 1, 90, 0, 28, 15, 1, 0, 0),
       (600063, 51006, 2, 90, 0, 31, 15, 1, 0, 0),
       (600064, 51006, 3, 90, 0, 27, 15, 1, 0, 0),
       (600071, 51007, 0, 160, 0, 42, 25, 1, 0, 0),
       (600072, 51007, 1, 160, 0, 39, 25, 1, 0, 0),
       (600073, 51007, 2, 160, 0, 44, 25, 1, 0, 0),
       (600074, 51007, 3, 160, 0, 38, 25, 1, 0, 0),
       (600081, 51008, 0, 36, 0, 17, 8, 1, 0, 0),
       (600082, 51008, 1, 36, 0, 16, 8, 1, 0, 0),
       (600083, 51008, 2, 36, 0, 14, 8, 1, 0, 0),
       (600084, 51008, 3, 36, 0, 15, 8, 1, 0, 0);

INSERT INTO stock_reservation (
    id, main_order_no, sub_order_no, sku_id, segment_id, quantity, status, idempotency_key, deleted, version
)
VALUES (61001, 'M2026000002', 'S2026000002', 51003, 0, 1, 'SOLD', 'idem-stock-61001', 0, 0),
       (61002, 'M2026000003', 'S2026000003', 51004, 1, 1, 'SOLD', 'idem-stock-61002', 0, 0),
       (61003, 'M2026000003', 'S2026000004', 51006, 2, 1, 'SOLD', 'idem-stock-61003', 0, 0),
       (61004, 'M2026000004', 'S2026000005', 51002, 0, 1, 'SOLD', 'idem-stock-61004', 0, 0),
       (61005, 'M2026000005', 'S2026000006', 51008, 1, 1, 'SOLD', 'idem-stock-61005', 0, 0),
       (61006, 'M2026000006', 'S2026000007', 51003, 2, 1, 'SOLD', 'idem-stock-61006', 0, 0);

INSERT INTO stock_txn (
    id, sku_id, segment_id, sub_order_no, txn_type, quantity, before_available, after_available,
    before_locked, after_locked, before_sold, after_sold, remark, deleted, version
)
VALUES (62001, 51003, 0, 'S2026000002', 'RESERVE', 1, 49, 48, 0, 1, 21, 21, 'Reserve stock for paid phone order.', 0, 0),
       (62002, 51003, 0, 'S2026000002', 'CONFIRM', 1, 48, 48, 1, 0, 21, 22, 'Confirm stock after payment success.', 0, 0),
       (62003, 51004, 1, 'S2026000003', 'RESERVE', 1, 21, 20, 0, 1, 8, 8, 'Reserve stock for foldable order.', 0, 0),
       (62004, 51004, 1, 'S2026000003', 'CONFIRM', 1, 20, 20, 1, 0, 8, 9, 'Confirm stock for delivered order.', 0, 0),
       (62005, 51006, 2, 'S2026000004', 'RESERVE', 1, 91, 90, 0, 1, 30, 30, 'Reserve stock for watch order.', 0, 0),
       (62006, 51006, 2, 'S2026000004', 'CONFIRM', 1, 90, 90, 1, 0, 30, 31, 'Confirm stock for watch order.', 0, 0),
       (62007, 51002, 0, 'S2026000005', 'RESERVE', 1, 76, 75, 0, 1, 7, 7, 'Reserve stock for repeat phone order.', 0, 0),
       (62008, 51002, 0, 'S2026000005', 'CONFIRM', 1, 75, 75, 1, 0, 7, 8, 'Confirm stock for repeat phone order.', 0, 0),
       (62009, 51008, 1, 'S2026000006', 'RESERVE', 1, 37, 36, 0, 1, 15, 15, 'Reserve stock for coffee machine order.', 0, 0),
       (62010, 51008, 1, 'S2026000006', 'CONFIRM', 1, 36, 36, 1, 0, 15, 16, 'Confirm stock for coffee machine order.', 0, 0),
       (62011, 51003, 2, 'S2026000007', 'RESERVE', 1, 49, 48, 0, 1, 18, 18, 'Reserve stock for premium phone order.', 0, 0),
       (62012, 51003, 2, 'S2026000007', 'CONFIRM', 1, 48, 48, 1, 0, 18, 19, 'Confirm stock for premium phone order.', 0, 0);
