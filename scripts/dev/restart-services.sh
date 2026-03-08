#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

SERVICES_FILTER=""
STOP_TIMEOUT=""
DRY_RUN=0

for arg in "$@"; do
  case "$arg" in
    --services=*) SERVICES_FILTER="${arg#*=}" ;;
    --timeout=*) STOP_TIMEOUT="${arg#*=}" ;;
    --dry-run) DRY_RUN=1 ;;
  esac
done

stop_args=()
if [ "$DRY_RUN" = "1" ]; then stop_args+=("--dry-run"); fi
if [ -n "$SERVICES_FILTER" ]; then stop_args+=("--services=$SERVICES_FILTER"); fi
if [ -n "$STOP_TIMEOUT" ]; then stop_args+=("--timeout=$STOP_TIMEOUT"); fi

echo "=== RESTART SERVICES ==="
bash "$SCRIPT_DIR/stop-services.sh" "${stop_args[@]}"
bash "$SCRIPT_DIR/start-services.sh" "$@"
