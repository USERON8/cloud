#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://host.docker.internal:18080}"
PROFILE="${2:-loadtest}"

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$ROOT_DIR"

echo "[k6] starting all-services smoke run..."
echo "[k6] BASE_URL=${BASE_URL}"

export K6_BASE_URL="$BASE_URL"
docker compose -f docker/monitoring-compose.yml --profile "$PROFILE" run --rm k6 run -o experimental-prometheus-rw /scripts/all-services-smoke.js
