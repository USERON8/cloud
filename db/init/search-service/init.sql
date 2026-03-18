DROP DATABASE IF EXISTS search_db;
CREATE DATABASE IF NOT EXISTS search_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE search_db;

CREATE TABLE undo_log (
    branch_id    BIGINT       NOT NULL,
    xid          VARCHAR(128) NOT NULL,
    context      VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB    NOT NULL,
    log_status   INT          NOT NULL,
    log_created  DATETIME     NOT NULL,
    log_modified DATETIME     NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id),
    INDEX idx_undo_log_xid (xid),
    INDEX idx_undo_log_branch (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS search_hot_keyword_total
(
    keyword       VARCHAR(128) NOT NULL,
    total_score   BIGINT       NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (keyword)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dead_letter
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    topic       VARCHAR(128)    NOT NULL,
    msg_id      VARCHAR(64)     NOT NULL,
    payload     TEXT            NOT NULL,
    fail_reason VARCHAR(32)     NOT NULL,
    error_msg   VARCHAR(512)    NULL,
    status      TINYINT         NOT NULL DEFAULT 0,
    service     VARCHAR(64)     NOT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at  DATETIME        NULL,
    UNIQUE KEY uk_dead_letter_msg (topic, msg_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
