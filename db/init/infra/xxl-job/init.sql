CREATE DATABASE IF NOT EXISTS xxl_job DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xxl_job;

CREATE TABLE IF NOT EXISTS xxl_job_group
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    app_name           VARCHAR(64)  NOT NULL,
    title              VARCHAR(64)  NOT NULL,
    address_type       TINYINT      NOT NULL DEFAULT 0,
    address_list       TEXT         NULL,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_xxl_job_group_app_name (app_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS xxl_job_info
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    job_group          INT          NOT NULL,
    job_desc           VARCHAR(255) NOT NULL,
    add_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    author             VARCHAR(64)  NULL,
    schedule_type      VARCHAR(50)  NOT NULL DEFAULT 'CRON',
    schedule_conf      VARCHAR(128) NULL,
    misfire_strategy   VARCHAR(50)  NOT NULL DEFAULT 'DO_NOTHING',
    executor_route_strategy VARCHAR(50) NOT NULL DEFAULT 'FIRST',
    executor_handler   VARCHAR(255) NOT NULL,
    executor_param     VARCHAR(512) NULL,
    executor_block_strategy VARCHAR(50) NOT NULL DEFAULT 'SERIAL_EXECUTION',
    executor_timeout   INT          NOT NULL DEFAULT 0,
    executor_fail_retry_count INT   NOT NULL DEFAULT 0,
    glue_type          VARCHAR(50)  NOT NULL DEFAULT 'BEAN',
    glue_source        MEDIUMTEXT   NULL,
    glue_remark        VARCHAR(128) NULL,
    glue_updatetime    DATETIME     NULL,
    child_jobid        VARCHAR(255) NULL,
    trigger_status     TINYINT      NOT NULL DEFAULT 0,
    trigger_last_time  BIGINT       NOT NULL DEFAULT 0,
    trigger_next_time  BIGINT       NOT NULL DEFAULT 0,
    INDEX idx_xxl_job_info_group_status (job_group, trigger_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS xxl_job_log
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_group          INT          NOT NULL,
    job_id             INT          NOT NULL,
    executor_address   VARCHAR(255) NULL,
    executor_handler   VARCHAR(255) NULL,
    executor_param     VARCHAR(512) NULL,
    executor_sharding_param VARCHAR(20) NULL,
    executor_fail_retry_count INT   NOT NULL DEFAULT 0,
    trigger_time       DATETIME     NULL,
    trigger_code       INT          NOT NULL DEFAULT 0,
    trigger_msg        TEXT         NULL,
    handle_time        DATETIME     NULL,
    handle_code        INT          NOT NULL DEFAULT 0,
    handle_msg         TEXT         NULL,
    alarm_status       TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_xxl_job_log_job_id (job_id),
    INDEX idx_xxl_job_log_trigger_time (trigger_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS xxl_job_log_report
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    trigger_day        DATETIME     NOT NULL,
    running_count      INT          NOT NULL DEFAULT 0,
    suc_count          INT          NOT NULL DEFAULT 0,
    fail_count         INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_xxl_job_log_report_day (trigger_day)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS xxl_job_registry
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    registry_group     VARCHAR(50)  NOT NULL,
    registry_key       VARCHAR(255) NOT NULL,
    registry_value     VARCHAR(255) NOT NULL,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_xxl_job_registry_key (registry_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS xxl_job_user
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    username           VARCHAR(50)  NOT NULL,
    password           VARCHAR(255) NOT NULL,
    role               TINYINT      NOT NULL,
    permission         VARCHAR(255) NULL,
    UNIQUE KEY uk_xxl_job_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO xxl_job_user (username, password, role, permission)
SELECT 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM xxl_job_user WHERE username = 'admin');

INSERT INTO xxl_job_group (app_name, title, address_type, address_list)
SELECT 'order-service', 'order-service', 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM xxl_job_group WHERE app_name = 'order-service');

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Cancel timeout orders',
    'system',
    'CRON',
    '0 */5 * * * ?',
    'DO_NOTHING',
    'FIRST',
    'orderTimeoutCheckJob',
    NULL,
    'SERIAL_EXECUTION',
    300,
    2,
    'BEAN',
    'seeded by init.sql',
    1,
    1,
    0
FROM xxl_job_group g
WHERE g.app_name = 'order-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'orderTimeoutCheckJob'
  );

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Auto confirm shipped orders',
    'system',
    'CRON',
    '0 0 * * * ?',
    'DO_NOTHING',
    'FIRST',
    'orderAutoConfirmReceiptJob',
    NULL,
    'SERIAL_EXECUTION',
    300,
    2,
    'BEAN',
    'seeded by init.sql',
    1,
    0,
    0
FROM xxl_job_group g
WHERE g.app_name = 'order-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'orderAutoConfirmReceiptJob'
  );

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Auto approve after-sale audits',
    'system',
    'CRON',
    '0 */30 * * * ?',
    'DO_NOTHING',
    'FIRST',
    'afterSaleAutoApproveJob',
    NULL,
    'SERIAL_EXECUTION',
    300,
    2,
    'BEAN',
    'seeded by init.sql',
    1,
    0,
    0
FROM xxl_job_group g
WHERE g.app_name = 'order-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'afterSaleAutoApproveJob'
  );

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Cleanup seata undo_log',
    'system',
    'CRON',
    '0 0 3 * * ?',
    'DO_NOTHING',
    'FIRST',
    'seataUndoLogCleanJob',
    NULL,
    'SERIAL_EXECUTION',
    600,
    0,
    'BEAN',
    'seeded by init.sql',
    1,
    0,
    0
FROM xxl_job_group g
WHERE g.app_name = 'order-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'seataUndoLogCleanJob'
  );

INSERT INTO xxl_job_group (app_name, title, address_type, address_list)
SELECT 'payment-service', 'payment-service', 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM xxl_job_group WHERE app_name = 'payment-service');

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Reconcile pending payment orders',
    'system',
    'CRON',
    '0 */2 * * * ?',
    'DO_NOTHING',
    'FIRST',
    'paymentOrderReconcileJob',
    NULL,
    'SERIAL_EXECUTION',
    300,
    2,
    'BEAN',
    'seeded by init.sql',
    1,
    0,
    0
FROM xxl_job_group g
WHERE g.app_name = 'payment-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'paymentOrderReconcileJob'
  );

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Retry payment refunds',
    'system',
    'CRON',
    '0 */5 * * * ?',
    'DO_NOTHING',
    'FIRST',
    'paymentRefundRetryJob',
    NULL,
    'SERIAL_EXECUTION',
    300,
    2,
    'BEAN',
    'seeded by init.sql',
    1,
    0,
    0
FROM xxl_job_group g
WHERE g.app_name = 'payment-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'paymentRefundRetryJob'
  );

INSERT INTO xxl_job_group (app_name, title, address_type, address_list)
SELECT 'search-service', 'search-service', 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM xxl_job_group WHERE app_name = 'search-service');

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Persist hot keywords',
    'system',
    'CRON',
    '0 0 * * * ?',
    'DO_NOTHING',
    'FIRST',
    'hotKeywordPersistJob',
    NULL,
    'SERIAL_EXECUTION',
    300,
    2,
    'BEAN',
    'seeded by init.sql',
    1,
    0,
    0
FROM xxl_job_group g
WHERE g.app_name = 'search-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'hotKeywordPersistJob'
  );

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Warmup hot keywords',
    'system',
    'CRON',
    '0 0 * * * ?',
    'DO_NOTHING',
    'FIRST',
    'hotKeywordWarmUpJob',
    NULL,
    'SERIAL_EXECUTION',
    300,
    2,
    'BEAN',
    'seeded by init.sql',
    1,
    0,
    0
FROM xxl_job_group g
WHERE g.app_name = 'search-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'hotKeywordWarmUpJob'
  );

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Rebuild search index',
    'system',
    'CRON',
    '0 0 4 * * ?',
    'DO_NOTHING',
    'FIRST',
    'esIndexRebuildJob',
    NULL,
    'SERIAL_EXECUTION',
    1800,
    0,
    'BEAN',
    'seeded by init.sql',
    0,
    0,
    0
FROM xxl_job_group g
WHERE g.app_name = 'search-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'esIndexRebuildJob'
  );

INSERT INTO xxl_job_group (app_name, title, address_type, address_list)
SELECT 'auth-service', 'auth-service', 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM xxl_job_group WHERE app_name = 'auth-service');

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Cleanup expired tokens',
    'system',
    'CRON',
    '0 0 2 * * ?',
    'DO_NOTHING',
    'FIRST',
    'authTokenCleanupJob',
    NULL,
    'SERIAL_EXECUTION',
    300,
    2,
    'BEAN',
    'seeded by init.sql',
    1,
    0,
    0
FROM xxl_job_group g
WHERE g.app_name = 'auth-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'authTokenCleanupJob'
  );

INSERT INTO xxl_job_group (app_name, title, address_type, address_list)
SELECT 'stock-service', 'stock-service', 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM xxl_job_group WHERE app_name = 'stock-service');

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Warmup stock cache',
    'system',
    'CRON',
    '0 0 3 * * ?',
    'DO_NOTHING',
    'FIRST',
    'stockCacheWarmUpJob',
    NULL,
    'SERIAL_EXECUTION',
    300,
    2,
    'BEAN',
    'seeded by init.sql',
    1,
    0,
    0
FROM xxl_job_group g
WHERE g.app_name = 'stock-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'stockCacheWarmUpJob'
  );

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    author,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_remark,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    g.id,
    'Verify stock cache',
    'system',
    'CRON',
    '0 0 * * * ?',
    'DO_NOTHING',
    'FIRST',
    'stockCacheVerifyJob',
    NULL,
    'SERIAL_EXECUTION',
    300,
    2,
    'BEAN',
    'seeded by init.sql',
    1,
    0,
    0
FROM xxl_job_group g
WHERE g.app_name = 'stock-service'
  AND NOT EXISTS (
      SELECT 1
      FROM xxl_job_info j
      WHERE j.job_group = g.id
        AND j.executor_handler = 'stockCacheVerifyJob'
  );
