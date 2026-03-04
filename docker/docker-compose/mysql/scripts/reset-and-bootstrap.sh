#!/bin/bash
set -euo pipefail

echo "[mysql-bootstrap] Reset datadir for deterministic re-init..."
rm -rf /var/lib/mysql/*

exec docker-entrypoint.sh mysqld \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci \
  --max_connections=600 \
  --innodb_buffer_pool_size=512M \
  --innodb_buffer_pool_instances=2 \
  --innodb_log_buffer_size=64M \
  --innodb_flush_log_at_trx_commit=2 \
  --thread_cache_size=64 \
  --table_open_cache=2048 \
  --server-id=1 \
  --log-bin=/var/lib/mysql/mysql-bin \
  --binlog_expire_logs_seconds=604800 \
  --max_binlog_size=268435456 \
  --binlog_format=ROW \
  --binlog_row_image=FULL
