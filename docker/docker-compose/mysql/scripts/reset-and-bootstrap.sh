#!/bin/bash
set -e
if set -o pipefail >/dev/null 2>&1; then
  set -o pipefail
fi

if compgen -G "/opt/mysql/config-source/*.cnf" >/dev/null; then
  echo "[mysql-bootstrap] Install container-readable MySQL configuration..."
  cp /opt/mysql/config-source/*.cnf /etc/mysql/conf.d/
  chmod 0644 /etc/mysql/conf.d/*.cnf
fi

echo "[mysql-bootstrap] Reset datadir for deterministic re-init..."
rm -rf /var/lib/mysql/*

exec docker-entrypoint.sh mysqld
