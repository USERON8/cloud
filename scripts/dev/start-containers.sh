#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/lib/port-guard.sh"

WITH_MONITORING=0
KILL_PORTS=1
DRY_RUN=0
for arg in "$@"; do
  case "$arg" in
    --with-monitoring) WITH_MONITORING=1 ;;
    --dry-run) DRY_RUN=1 ;;
    --kill-ports) KILL_PORTS=1 ;;
    --no-kill-ports) KILL_PORTS=0 ;;
  esac
done

ENV_FILE="$ROOT_DIR/docker/.env"
if [ ! -f "$ENV_FILE" ]; then
  echo "docker/.env not found" >&2
  exit 1
fi

mapfile -t PORTS < <(awk -F= '/^PORT_[A-Z0-9_]+=/{print $2}' "$ENV_FILE")

if [ "$KILL_PORTS" = "1" ]; then
  docker_owner=0
  for p in "${PORTS[@]}"; do
    if is_docker_owner "$p"; then
      docker_owner=1
      break
    fi
  done

  if [ "$docker_owner" = "1" ] && [ "$DRY_RUN" = "0" ]; then
    (
      cd "$ROOT_DIR/docker"
      docker compose -f docker-compose.yml down >/dev/null 2>&1 || true
      docker compose -f monitoring-compose.yml down >/dev/null 2>&1 || true
    )
  fi

  for p in "${PORTS[@]}"; do
    kill_port_owner "$p" "$DRY_RUN"
  done
fi

if [ "$DRY_RUN" = "1" ]; then
  echo "DRY_RUN_DONE script=start-containers"
  exit 0
fi

cd "$ROOT_DIR/docker"
docker compose -f docker-compose.yml up -d
if [ "$WITH_MONITORING" = "1" ]; then
  docker compose -f monitoring-compose.yml up -d prometheus grafana
fi

echo "CONTAINERS_START withMonitoring=$WITH_MONITORING status=started"
