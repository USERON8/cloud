#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

WITH_MONITORING=0
SKIP_CONTAINERS=0
SKIP_SERVICES=0
SERVICES_FILTER=""
STOP_TIMEOUT=""
DRY_RUN=0

for arg in "$@"; do
  case "$arg" in
    --with-monitoring) WITH_MONITORING=1 ;;
    --skip-containers) SKIP_CONTAINERS=1 ;;
    --skip-services) SKIP_SERVICES=1 ;;
    --services=*) SERVICES_FILTER="${arg#*=}" ;;
    --timeout=*) STOP_TIMEOUT="${arg#*=}" ;;
    --dry-run) DRY_RUN=1 ;;
    --open-dashboards|--enable-skywalking|--no-kill-ports) : ;;
    --skywalking-agent-path=*|--skywalking-backend=*) : ;;
  esac
done

invoke_step() {
  local script_path="$1"
  local error_message="$2"
  shift 2
  bash "$script_path" "$@"
  local exit_code=$?
  if [ "$exit_code" -ne 0 ]; then
    echo "$error_message" >&2
    exit "$exit_code"
  fi
}

echo "=== STOP PLATFORM ==="

service_args=()
if [ "$DRY_RUN" = "1" ]; then service_args+=("--dry-run"); fi
if [ -n "$SERVICES_FILTER" ]; then service_args+=("--services=$SERVICES_FILTER"); fi
if [ -n "$STOP_TIMEOUT" ]; then service_args+=("--timeout=$STOP_TIMEOUT"); fi

container_args=()
if [ "$DRY_RUN" = "1" ]; then container_args+=("--dry-run"); fi
if [ "$WITH_MONITORING" = "1" ]; then container_args+=("--with-monitoring"); fi

if [ "$SKIP_SERVICES" = "0" ]; then
  echo "STEP 1/2 services=stop"
  invoke_step "$SCRIPT_DIR/stop-services.sh" "Service stop failed" "${service_args[@]}"
else
  echo "STEP 1/2 services=skipped"
fi

if [ "$SKIP_CONTAINERS" = "0" ]; then
  echo "STEP 2/2 containers=stop"
  invoke_step "$SCRIPT_DIR/stop-containers.sh" "Container stop failed" "${container_args[@]}"
else
  echo "STEP 2/2 containers=skipped"
fi

echo "PLATFORM_STOPPED"
