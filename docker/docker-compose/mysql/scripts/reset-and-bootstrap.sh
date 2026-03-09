#!/bin/bash
set -eo pipefail

echo "[mysql-bootstrap] Reset datadir for deterministic re-init..."
rm -rf /var/lib/mysql/*

exec docker-entrypoint.sh mysqld
