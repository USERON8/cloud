-- RocketMQ acceptance schema compatibility patch
-- Date: 2026-02-23

USE mysql;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_col_if_missing $$
CREATE PROCEDURE add_col_if_missing(
    IN p_schema VARCHAR(64),
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = p_schema
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', p_schema, '`.`', p_table, '` ',
            'ADD COLUMN `', p_column, '` ', p_definition
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL add_col_if_missing('order_db', 'orders', 'order_no', 'VARCHAR(64) NULL');
CALL add_col_if_missing('order_db', 'orders', 'refund_status', 'TINYINT NULL DEFAULT 0');
CALL add_col_if_missing('order_db', 'orders', 'pay_time', 'DATETIME NULL');
CALL add_col_if_missing('order_db', 'orders', 'ship_time', 'DATETIME NULL');
CALL add_col_if_missing('order_db', 'orders', 'complete_time', 'DATETIME NULL');
CALL add_col_if_missing('order_db', 'orders', 'cancel_time', 'DATETIME NULL');
CALL add_col_if_missing('order_db', 'orders', 'cancel_reason', 'VARCHAR(255) NULL');
CALL add_col_if_missing('order_db', 'orders', 'remark', 'VARCHAR(255) NULL');
CALL add_col_if_missing('order_db', 'orders', 'shop_id', 'BIGINT UNSIGNED NULL');
CALL add_col_if_missing('order_db', 'orders', 'version', 'INT NOT NULL DEFAULT 0');

CALL add_col_if_missing('user_db', 'users', 'github_id', 'BIGINT UNSIGNED NULL');
CALL add_col_if_missing('user_db', 'users', 'github_username', 'VARCHAR(100) NULL');
CALL add_col_if_missing('user_db', 'users', 'oauth_provider', 'VARCHAR(32) NULL');
CALL add_col_if_missing('user_db', 'users', 'oauth_provider_id', 'VARCHAR(100) NULL');
CALL add_col_if_missing('user_db', 'users', 'version', 'INT NOT NULL DEFAULT 0');

CALL add_col_if_missing('payment_db', 'payment', 'version', 'INT NOT NULL DEFAULT 0');

CALL add_col_if_missing('stock_db', 'stock', 'low_stock_threshold', 'INT NOT NULL DEFAULT 0');
CALL add_col_if_missing('stock_db', 'stock_in', 'version', 'INT NOT NULL DEFAULT 0');
CALL add_col_if_missing('stock_db', 'stock_out', 'version', 'INT NOT NULL DEFAULT 0');

CALL add_col_if_missing('product_db', 'products', 'version', 'INT NOT NULL DEFAULT 0');

DROP PROCEDURE IF EXISTS add_col_if_missing;
