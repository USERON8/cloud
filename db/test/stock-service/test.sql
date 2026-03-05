USE stock_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM stock_log;
DELETE FROM stock_count;
DELETE FROM stock_out;
DELETE FROM stock_in;
DELETE FROM stock;

INSERT INTO stock (id, product_id, product_name, stock_quantity, frozen_quantity, stock_status, low_stock_threshold, deleted, version)
VALUES (50001, 40001, 'Test Product A', 100, 0, 1, 5, 0, 0);

INSERT INTO stock_in (id, product_id, quantity, deleted, version)
VALUES (51001, 40001, 100, 0, 0);

INSERT INTO stock_out (id, product_id, order_id, quantity, deleted, version)
VALUES (52001, 40001, 12001, 1, 0, 0);
