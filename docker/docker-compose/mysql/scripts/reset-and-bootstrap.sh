#!/bin/bash
set -e
if set -o pipefail >/dev/null 2>&1; then
  set -o pipefail
fi

echo "[mysql-bootstrap] Reset datadir for deterministic re-init..."
rm -rf /var/lib/mysql/*
