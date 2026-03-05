#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
PIDS_FILE="${ROOT_DIR}/.tmp/acceptance/pids.txt"
PREV_PIDS_FILE="${ROOT_DIR}/.tmp/acceptance/pids.before-deploy.txt"

stop_by_pid_file() {
  local file="$1"
  [ -f "$file" ] || return 0
  while IFS=',' read -r _ _ pid _; do
    if [ -n "${pid:-}" ] && kill -0 "$pid" >/dev/null 2>&1; then
      kill "$pid" || true
    fi
  done < "$file"
}

echo "Rollback: stop current deployed service processes"
stop_by_pid_file "$PIDS_FILE"

echo "Rollback: clean ports and restart previous snapshot if present"
if [ -f "$PREV_PIDS_FILE" ]; then
  cp "$PREV_PIDS_FILE" "$PIDS_FILE"
fi

echo "Rollback completed (processes stopped)."
