USE stock_db;

DELETE FROM stock_count WHERE id BETWEEN 50001 AND 50050;
DELETE FROM stock_log WHERE id BETWEEN 50001 AND 50050;
DELETE FROM stock_out WHERE id BETWEEN 50001 AND 50050;
DELETE FROM stock_in WHERE id BETWEEN 50001 AND 50050;
DELETE FROM stock WHERE id BETWEEN 50001 AND 50050;

INSERT INTO stock (id, product_id, product_name, stock_quantity, frozen_quantity, stock_status, low_stock_threshold, deleted, version)
VALUES (50001, 40001, 'Test Phone A', 100, 5, 1, 10, 0, 0),
       (50002, 40002, 'Test Laptop A', 40, 2, 1, 5, 0, 0),
       (50003, 40003, 'Test Phone B', 15, 3, 2, 10, 0, 0);

INSERT INTO stock_in (id, product_id, quantity, deleted, version)
VALUES (50001, 40001, 100, 0, 0),
       (50002, 40002, 40, 0, 0),
       (50003, 40003, 15, 0, 0);

INSERT INTO stock_out (id, product_id, order_id, quantity, deleted, version)
VALUES (50001, 40001, 20001, 1, 0, 0),
       (50002, 40002, 20003, 1, 0, 0);

INSERT INTO stock_log (id, product_id, product_name, operation_type, quantity_before, quantity_after, quantity_change,
                       order_id, order_no, operator_id, operator_name, remark, operate_time, ip_address, deleted, version)
VALUES (50001, 40001, 'Test Phone A', 'IN', 0, 100, 100, NULL, NULL, 1, 'system', 'init in', NOW(), '127.0.0.1', 0, 0),
       (50002, 40001, 'Test Phone A', 'OUT', 100, 99, -1, 20001, 'ORD-T-20001', 1, 'system', 'order out', NOW(), '127.0.0.1', 0, 0),
       (50003, 40003, 'Test Phone B', 'RESERVE', 15, 12, -3, 20004, 'ORD-T-20004', 1, 'system', 'reserve', NOW(), '127.0.0.1', 0, 0);

INSERT INTO stock_count (id, count_no, product_id, product_name, expected_quantity, actual_quantity, difference, status,
                         operator_id, operator_name, confirm_user_id, confirm_user_name, count_time, confirm_time, remark, deleted, version)
VALUES (50001, 'COUNT-T-50001', 40001, 'Test Phone A', 99, 99, 0, 'CONFIRMED',
        1, 'counter', 2, 'manager', NOW(), NOW(), 'ok', 0, 0),
       (50002, 'COUNT-T-50002', 40003, 'Test Phone B', 12, 10, -2, 'PENDING',
        1, 'counter', NULL, NULL, NOW(), NULL, 'pending confirm', 0, 0);
