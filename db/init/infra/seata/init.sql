CREATE DATABASE seata CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

use seata;

CREATE TABLE global_table (
  xid VARCHAR(128) NOT NULL,
  transaction_id BIGINT,
  status TINYINT NOT NULL,
  application_id VARCHAR(32),
  transaction_service_group VARCHAR(32),
  transaction_name VARCHAR(128),
  timeout INT,
  begin_time BIGINT,
  application_data VARCHAR(2000),
  gmt_create DATETIME,
  gmt_modified DATETIME,
  PRIMARY KEY (xid),
  KEY idx_status_gmt_modified (status,gmt_modified),
  KEY idx_transaction_id (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE branch_table (
  branch_id BIGINT NOT NULL,
  xid VARCHAR(128) NOT NULL,
  transaction_id BIGINT ,
  resource_group_id VARCHAR(32),
  resource_id VARCHAR(256) ,
  branch_type VARCHAR(8) ,
  status TINYINT,
  client_id VARCHAR(64),
  application_data VARCHAR(2000),
  gmt_create DATETIME,
  gmt_modified DATETIME,
  PRIMARY KEY (branch_id),
  KEY idx_xid (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE lock_table (
  row_key VARCHAR(128) NOT NULL,
  xid VARCHAR(128),
  transaction_id BIGINT,
  branch_id BIGINT NOT NULL,
  resource_id VARCHAR(256),
  table_name VARCHAR(32),
  pk VARCHAR(36),
  status TINYINT NOT NULL DEFAULT '0',
  gmt_create DATETIME,
  gmt_modified DATETIME,
  PRIMARY KEY (row_key),
  KEY idx_status (status),
  KEY idx_branch_id (branch_id),
  KEY idx_xid (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
