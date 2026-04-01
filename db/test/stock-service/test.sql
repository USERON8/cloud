USE stock_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM stock_txn;
DELETE FROM stock_reservation;
DELETE FROM stock_segment;

INSERT INTO stock_segment (
    id, sku_id, segment_id, available_qty, locked_qty, sold_qty, alert_threshold, status, deleted, version
)
VALUES (600011, 51001, 0, 125, 0, 0, 20, 1, 0, 0),
       (600012, 51001, 1, 125, 0, 0, 20, 1, 0, 0),
       (600013, 51001, 2, 125, 0, 0, 20, 1, 0, 0),
       (600014, 51001, 3, 125, 0, 0, 20, 1, 0, 0),
       (600021, 51002, 0, 75, 0, 0, 20, 1, 0, 0),
       (600022, 51002, 1, 75, 0, 0, 20, 1, 0, 0),
       (600023, 51002, 2, 75, 0, 0, 20, 1, 0, 0),
       (600024, 51002, 3, 75, 0, 0, 20, 1, 0, 0);
