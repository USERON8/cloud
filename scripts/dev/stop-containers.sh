#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

DRY_RUN=0
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    --with-monitoring) : ;;
  esac
done

if [ "$DRY_RUN" = "1" ]; then
  echo "DRY_RUN_DONE script=stop-containers"
  exit 0
fi

cd "$ROOT_DIR/docker"
if [ -f monitoring-compose.yml ]; then
  docker compose -f monitoring-compose.yml down --remove-orphans >/dev/null 2>&1 || true
fi
docker compose -f docker-compose.yml down --remove-orphans >/dev/null 2>&1 || true

echo "CONTAINERS_STOP status=stopped"
