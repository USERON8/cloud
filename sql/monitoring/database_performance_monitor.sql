-- ==================== 数据库性能监控脚本 ====================
-- 用于监控索引优化效果和数据库性能指标

-- 创建性能监控数据库
CREATE DATABASE IF NOT EXISTS `performance_monitor`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `performance_monitor`;

-- 创建慢查询记录表
CREATE TABLE IF NOT EXISTS `slow_query_log`
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_time    DECIMAL(12, 6) NOT NULL COMMENT '查询时间（秒）',
    lock_time     DECIMAL(12, 6) NOT NULL COMMENT '锁定时间（秒）',
    rows_sent     INT            NOT NULL COMMENT '发送行数',
    rows_examined INT            NOT NULL COMMENT '检查行数',
    database      VARCHAR(64) COMMENT '数据库名',
    sql_text      TEXT COMMENT 'SQL语句',
    timestamp     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    INDEX idx_timestamp (timestamp),
    INDEX idx_query_time (query_time),
    INDEX idx_database (database)
) COMMENT ='慢查询记录表';

-- 创建索引使用统计表
CREATE TABLE IF NOT EXISTS `index_usage_stats`
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_schema  VARCHAR(64) NOT NULL COMMENT '表模式',
    table_name    VARCHAR(64) NOT NULL COMMENT '表名',
    index_name    VARCHAR(64) NOT NULL COMMENT '索引名',
    cardinality   BIGINT COMMENT '基数',
    table_rows    BIGINT COMMENT '表行数',
    selectivity   DECIMAL(5, 2) COMMENT '选择性百分比',
    index_size_mb DECIMAL(10, 2) COMMENT '索引大小(MB)',
    record_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    INDEX idx_record_time (record_time),
    INDEX idx_table_schema (table_schema),
    INDEX idx_table_name (table_name)
) COMMENT ='索引使用统计表';

-- 创建数据库性能指标表
CREATE TABLE IF NOT EXISTS `db_performance_metrics`
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name   VARCHAR(100)   NOT NULL COMMENT '指标名称',
    metric_value  DECIMAL(15, 2) NOT NULL COMMENT '指标值',
    metric_unit   VARCHAR(20) COMMENT '指标单位',
    database_name VARCHAR(64) COMMENT '数据库名',
    table_name    VARCHAR(64) COMMENT '表名',
    record_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    INDEX idx_record_time (record_time),
    INDEX idx_metric_name (metric_name),
    INDEX idx_database (database_name)
) COMMENT ='数据库性能指标表';

-- ==================== 性能监控存储过程 ====================

-- 收集索引使用统计
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS collect_index_usage_stats()
BEGIN
    -- 清空临时表
    TRUNCATE TABLE index_usage_stats;

    -- 收集索引统计信息
    INSERT INTO index_usage_stats (table_schema, table_name, index_name, cardinality, table_rows, selectivity,
                                   index_size_mb)
    SELECT s.TABLE_SCHEMA,
           s.TABLE_NAME,
           s.INDEX_NAME,
           s.CARDINALITY,
           t.TABLE_ROWS,
           CASE
               WHEN t.TABLE_ROWS > 0 THEN ROUND((s.CARDINALITY / t.TABLE_ROWS) * 100, 2)
               ELSE 0
               END                                  as selectivity,
           ROUND((s.INDEX_LENGTH / 1024 / 1024), 2) as index_size_mb
    FROM information_schema.STATISTICS s
             JOIN information_schema.TABLES t ON s.TABLE_SCHEMA = t.TABLE_SCHEMA AND s.TABLE_NAME = t.TABLE_NAME
    WHERE s.TABLE_SCHEMA IN ('user_db', 'order_db', 'product_db', 'payment_db', 'stock_db')
      AND s.INDEX_NAME != 'PRIMARY'
    ORDER BY s.TABLE_SCHEMA, s.TABLE_NAME, s.CARDINALITY DESC;

    SELECT CONCAT('索引统计收集完成，记录数: ', ROW_COUNT()) as result;
END //
DELIMITER ;

-- 收集数据库性能指标
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS collect_db_performance_metrics()
BEGIN
    DECLARE total_connections INT;
    DECLARE active_connections INT;
    DECLARE slow_queries_count INT;
    DECLARE qps DECIMAL(10, 2);

    -- 获取连接数
    SELECT VARIABLE_VALUE
    INTO total_connections
    FROM performance_schema.global_status
    WHERE VARIABLE_NAME = 'Threads_connected';

    -- 获取活跃连接数
    SELECT VARIABLE_VALUE
    INTO active_connections
    FROM performance_schema.global_status
    WHERE VARIABLE_NAME = 'Threads_running';

    -- 获取慢查询数
    SELECT VARIABLE_VALUE
    INTO slow_queries_count
    FROM performance_schema.global_status
    WHERE VARIABLE_NAME = 'Slow_queries';

    -- 计算QPS
    SELECT VARIABLE_VALUE
    INTO @questions
    FROM performance_schema.global_status
    WHERE VARIABLE_NAME = 'Questions';

    SELECT VARIABLE_VALUE
    INTO @uptime
    FROM performance_schema.global_status
    WHERE VARIABLE_NAME = 'Uptime';

    SET qps = @questions / @uptime;

    -- 插入性能指标
    INSERT INTO db_performance_metrics (metric_name, metric_value, metric_unit)
    VALUES ('total_connections', total_connections, 'count'),
           ('active_connections', active_connections, 'count'),
           ('slow_queries_count', slow_queries_count, 'count'),
           ('queries_per_second', qps, 'queries/sec');

    SELECT CONCAT('性能指标收集完成') as result;
END //
DELIMITER ;

-- 分析慢查询
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS analyze_slow_queries()
BEGIN
    -- 获取最近24小时的慢查询统计
    SELECT database,
           COUNT(*)           as slow_query_count,
           AVG(query_time)    as avg_query_time,
           MAX(query_time)    as max_query_time,
           AVG(rows_examined) as avg_rows_examined
    FROM slow_query_log
    WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
    GROUP BY database
    ORDER BY slow_query_count DESC;

    -- 获取最慢的10个查询
    SELECT database,
           query_time,
           rows_examined,
           LEFT(sql_text, 100) as sql_preview,
           timestamp
    FROM slow_query_log
    WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
    ORDER BY query_time DESC
    LIMIT 10;
END //
DELIMITER ;

-- ==================== 性能监控函数 ====================

DELIMITER //
CREATE FUNCTION IF NOT EXISTS get_table_size_mb(db_name VARCHAR(64), table_name VARCHAR(64))
    RETURNS DECIMAL(15, 2)
    READS SQL DATA
    DETERMINISTIC
BEGIN
    DECLARE table_size DECIMAL(15, 2);

    SELECT ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2)
    INTO table_size
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = db_name
      AND TABLE_NAME = table_name;

    RETURN table_size;
END //
DELIMITER ;

DELIMITER //
CREATE FUNCTION IF NOT EXISTS get_index_selectivity(db_name VARCHAR(64), table_name VARCHAR(64), index_name VARCHAR(64))
    RETURNS DECIMAL(5, 2)
    READS SQL DATA
    DETERMINISTIC
BEGIN
    DECLARE selectivity DECIMAL(5, 2);
    DECLARE table_rows BIGINT;
    DECLARE cardinality BIGINT;

    SELECT TABLE_ROWS
    INTO table_rows
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = db_name
      AND TABLE_NAME = table_name;

    SELECT CARDINALITY
    INTO cardinality
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = db_name
      AND TABLE_NAME = table_name
      AND INDEX_NAME = index_name;

    IF table_rows > 0 THEN
        SET selectivity = ROUND((cardinality / table_rows) * 100, 2);
    ELSE
        SET selectivity = 0;
    END IF;

    RETURN selectivity;
END //
DELIMITER ;

-- ==================== 性能监控视图 ====================

-- 优化建议视图
CREATE OR REPLACE VIEW v_optimization_recommendations AS
SELECT 'Low Selectivity Index'    as issue_type,
       CONCAT('索引 ', i.TABLE_SCHEMA, '.', i.TABLE_NAME, '.', i.INDEX_NAME, ' 选择性过低 (', s.selectivity,
              '%)')               as description,
       '考虑删除或重新设计此索引' as recommendation,
       'Medium'                   as priority
FROM information_schema.STATISTICS i
         JOIN index_usage_stats s ON i.TABLE_SCHEMA = s.table_schema
    AND i.TABLE_NAME = s.table_name
    AND i.INDEX_NAME = s.index_name
WHERE s.selectivity < 5
  AND i.INDEX_NAME != 'PRIMARY'

UNION ALL

SELECT 'Unused Index'                                                                     as issue_type,
       CONCAT('索引 ', s.table_schema, '.', s.table_name, '.', s.index_name, ' 基数为 0') as description,
       '考虑删除未使用的索引'                                                             as recommendation,
       'High'                                                                             as priority
FROM index_usage_stats s
WHERE s.cardinality = 0
  AND s.index_name != 'PRIMARY'

UNION ALL

SELECT 'Large Index'                    as issue_type,
       CONCAT('索引 ', s.table_schema, '.', s.table_name, '.', s.index_name, ' 占用空间过大 (', s.index_size_mb,
              ' MB)')                   as description,
       '考虑优化索引设计或使用覆盖索引' as recommendation,
       'Low'                            as priority
FROM index_usage_stats s
WHERE s.index_size_mb > 100;

-- 性能趋势视图
CREATE OR REPLACE VIEW v_performance_trend AS
SELECT metric_name,
       DATE(record_time) as metric_date,
       AVG(metric_value) as avg_value,
       MIN(metric_value) as min_value,
       MAX(metric_value) as max_value
FROM db_performance_metrics
WHERE record_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY metric_name, DATE(record_time)
ORDER BY metric_name, metric_date;

-- ==================== 定时任务设置 ====================

-- 创建事件调度器（需要启用事件调度器）
-- SET GLOBAL event_scheduler = ON;

-- 创建定时收集索引统计的事件
/*
CREATE EVENT IF NOT EXISTS collect_index_stats_event
ON SCHEDULE EVERY 1 HOUR
DO CALL collect_index_usage_stats();
*/

-- 创建定时收集性能指标的事件
/*
CREATE EVENT IF NOT EXISTS collect_performance_metrics_event
ON SCHEDULE EVERY 5 MINUTE
DO CALL collect_db_performance_metrics();
*/

-- ==================== 性能报告查询 ====================

-- 生成日性能报告
SELECT '数据库性能日报'                                                                as report_type,
       DATE(NOW())                                                                     as report_date,
       CONCAT('总连接数: ',
              MAX(CASE WHEN metric_name = 'total_connections' THEN metric_value END))  as connections_info,
       CONCAT('慢查询数: ',
              MAX(CASE WHEN metric_name = 'slow_queries_count' THEN metric_value END)) as slow_queries_info,
       CONCAT('平均QPS: ', ROUND(AVG(CASE WHEN metric_name = 'queries_per_second' THEN metric_value END),
                                 2))                                                   as qps_info,
       CONCAT('索引总数: ', COUNT(*))                                                  as index_count_info
FROM db_performance_metrics
WHERE record_time >= DATE(NOW())
UNION ALL

-- 生成索引使用报告
SELECT '索引使用报告'                                                                  as report_type,
       DATE(NOW())                                                                     as report_date,
       CONCAT('高选择性索引: ', SUM(CASE WHEN selectivity >= 80 THEN 1 ELSE 0 END))    as high_selectivity,
       CONCAT('中等等选择性索引: ',
              SUM(CASE WHEN selectivity >= 20 AND selectivity < 80 THEN 1 ELSE 0 END)) as medium_selectivity,
       CONCAT('低选择性索引: ', SUM(CASE WHEN selectivity < 20 THEN 1 ELSE 0 END))     as low_selectivity,
       CONCAT('索引总大小: ', ROUND(SUM(index_size_mb), 2), ' MB')                     as total_size
FROM index_usage_stats;

-- ==================== 使用说明 ====================

/*
使用方法：

1. 启用事件调度器：
   SET GLOBAL event_scheduler = ON;

2. 手动收集统计信息：
   CALL collect_index_usage_stats();
   CALL collect_db_performance_metrics();

3. 查看慢查询分析：
   CALL analyze_slow_queries();

4. 查看优化建议：
   SELECT * FROM v_optimization_recommendations;

5. 查看性能趋势：
   SELECT * FROM v_performance_trend WHERE metric_date >= DATE_SUB(NOW(), INTERVAL 7 DAY);

6. 生成性能报告：
   使用上面的性能报告查询

7. 清理历史数据（保留最近30天）：
   DELETE FROM slow_query_log WHERE timestamp < DATE_SUB(NOW(), INTERVAL 30 DAY);
   DELETE FROM index_usage_stats WHERE record_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
   DELETE FROM db_performance_metrics WHERE record_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
*/

COMMIT;