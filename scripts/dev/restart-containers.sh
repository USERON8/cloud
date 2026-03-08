#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

stop_args=()
for arg in "$@"; do
  case "$arg" in
    --dry-run|--with-monitoring) stop_args+=("$arg") ;;
  esac
done

echo "=== RESTART CONTAINERS ==="
bash "$SCRIPT_DIR/stop-containers.sh" "${stop_args[@]}"
bash "$SCRIPT_DIR/start-containers.sh" "$@"
