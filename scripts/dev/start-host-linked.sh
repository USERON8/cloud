#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
DRY_RUN=0

stop_cluster_app_containers() {
  local dry_run="$1"
  local services="nginx,gateway,auth-service,user-service,order-service,product-service,stock-service,payment-service,search-service"
  if [ "$dry_run" = "1" ]; then
    echo "CLUSTER_STOP services=${services} action=dry-run"
    return 0
  fi
  if ! command -v docker >/dev/null 2>&1; then
    return 0
  fi
  if ! docker version >/dev/null 2>&1; then
    return 0
  fi

  (
    cd "$ROOT_DIR/docker"
    docker compose -f docker-compose.yml --profile services rm -sf \
      nginx gateway auth-service user-service order-service product-service stock-service payment-service search-service >/dev/null 2>&1 || true
  )
}

has_critical_error() {
  local path="$1"
  [ -f "$path" ] || return 1
  grep -Eq 'APPLICATION FAILED TO START|Exception in thread|Application run failed|Web server failed to start|OutOfMemoryError' "$path"
}

assert_host_acceptance() {
  local requested_services="$1"
  local startup_file="$ROOT_DIR/.tmp/acceptance/startup.csv"
  [ -f "$startup_file" ] || {
    echo "startup acceptance file missing: $startup_file" >&2
    exit 1
  }

  local rows
  rows="$(tail -n +2 "$startup_file")"
  [ -n "$rows" ] || {
    echo "no accepted services found in $startup_file" >&2
    exit 1
  }

  local selected_filter=",${requested_services},"
  local matched=0
  while IFS=',' read -r service port pid status startup_seconds out_log err_log; do
    [ -n "$service" ] || continue
    if [ -n "$requested_services" ] && [[ "$selected_filter" != *",$service,"* ]]; then
      continue
    fi
    matched=1
    if [[ "$status" != "UP" && "$status" != "UP_SECURED" ]]; then
      echo "service not healthy after host startup: $service status=$status" >&2
      exit 1
    fi
    if [ -n "$err_log" ] && [ -f "$err_log" ] && [ -s "$err_log" ]; then
      echo "stderr is not empty: $service log=$err_log" >&2
      exit 1
    fi
    if [ -n "$out_log" ] && has_critical_error "$out_log"; then
      echo "critical error pattern found in stdout log: $service log=$out_log" >&2
      exit 1
    fi
  done <<< "$rows"

  if [ "$matched" != "1" ]; then
    echo "no selected services found in $startup_file" >&2
    exit 1
  fi
}

requested_services=""
for arg in "$@"; do
  case "$arg" in
    --services=*) requested_services="${arg#*=}" ;;
    --dry-run) DRY_RUN=1 ;;
  esac
done

stop_cluster_app_containers "$DRY_RUN"

bash "$SCRIPT_DIR/start-platform.sh" "$@"
if [ "$DRY_RUN" = "1" ]; then
  echo "DRY_RUN_DONE script=start-host-linked requestedServices=${requested_services}"
  exit 0
fi
assert_host_acceptance "$requested_services"
echo "HOST_LINKED_READY"
