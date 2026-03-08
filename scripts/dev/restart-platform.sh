#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

SKIP_CONTAINERS=0
SKIP_SERVICES=0
SERVICES_FILTER=""
STOP_TIMEOUT=""
DRY_RUN=0
WITH_MONITORING=0

for arg in "$@"; do
  case "$arg" in
    --skip-containers) SKIP_CONTAINERS=1 ;;
    --skip-services) SKIP_SERVICES=1 ;;
    --services=*) SERVICES_FILTER="${arg#*=}" ;;
    --timeout=*) STOP_TIMEOUT="${arg#*=}" ;;
    --dry-run) DRY_RUN=1 ;;
    --with-monitoring) WITH_MONITORING=1 ;;
  esac
done

stop_args=()
if [ "$DRY_RUN" = "1" ]; then stop_args+=("--dry-run"); fi
if [ "$WITH_MONITORING" = "1" ]; then stop_args+=("--with-monitoring"); fi
if [ "$SKIP_CONTAINERS" = "1" ]; then stop_args+=("--skip-containers"); fi
if [ "$SKIP_SERVICES" = "1" ]; then stop_args+=("--skip-services"); fi
if [ -n "$SERVICES_FILTER" ]; then stop_args+=("--services=$SERVICES_FILTER"); fi
if [ -n "$STOP_TIMEOUT" ]; then stop_args+=("--timeout=$STOP_TIMEOUT"); fi

echo "=== RESTART PLATFORM ==="
bash "$SCRIPT_DIR/stop-platform.sh" "${stop_args[@]}"
bash "$SCRIPT_DIR/start-platform.sh" "$@"
