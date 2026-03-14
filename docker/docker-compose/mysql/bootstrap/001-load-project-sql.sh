#!/bin/bash
set -e
if set -o pipefail >/dev/null 2>&1; then
  set -o pipefail
fi

run_sql_dir() {
  local dir="$1"
  local label="$2"
  if [ ! -d "$dir" ]; then
    echo "[mysql-bootstrap] Skip ${label}, directory not found: ${dir}"
    return
  fi

  echo "[mysql-bootstrap] Loading ${label} SQL from ${dir}"
  while IFS= read -r -d '' sql_file; do
    echo "[mysql-bootstrap] -> ${sql_file}"
    mysql --default-character-set=utf8mb4 -uroot -p"${MYSQL_ROOT_PASSWORD}" < "${sql_file}"
  done < <(find "${dir}" -type f -name "*.sql" -print0 | sort -z)
}

run_sql_dir "/bootstrap/db/init" "init"
run_sql_dir "/bootstrap/db/test" "test"
