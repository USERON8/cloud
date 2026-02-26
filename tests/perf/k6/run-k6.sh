#!/usr/bin/env bash
set -euo pipefail

SCENARIO="${1:-acceptance}"
BASE_URL="${2:-http://host.docker.internal:18080}"
PROFILE="${3:-loadtest}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$ROOT_DIR"
. "$SCRIPT_DIR/lib/preflight.sh"

MODE="all"
SCRIPT_PATH="/scripts/acceptance-cases.js"
DISPLAY_NAME="acceptance"

case "$SCENARIO" in
  acceptance)
    MODE="all"
    SCRIPT_PATH="/scripts/acceptance-cases.js"
    DISPLAY_NAME="acceptance"
    ;;
  smoke)
    MODE="all"
    SCRIPT_PATH="/scripts/all-services-smoke.js"
    DISPLAY_NAME="all-services smoke"
    ;;
  search-chain)
    MODE="search"
    SCRIPT_PATH="/scripts/search-chain.js"
    DISPLAY_NAME="search-chain"
    ;;
  search-max)
    MODE="search"
    SCRIPT_PATH="/scripts/search-singleton-max.js"
    DISPLAY_NAME="search singleton max"
    ;;
  route-only)
    MODE="all"
    SCRIPT_PATH="/scripts/gateway-route-only.js"
    DISPLAY_NAME="gateway route-only"
    ;;
  order-only)
    MODE="all"
    SCRIPT_PATH="/scripts/order-create-only.js"
    DISPLAY_NAME="order-create only"
    ;;
  *)
    echo "[k6] unsupported scenario: $SCENARIO" >&2
    echo "[k6] supported: acceptance|smoke|search-chain|search-max|route-only|order-only" >&2
    exit 1
    ;;
esac

k6_preflight "$MODE"

echo "[k6] scenario=${SCENARIO}"
echo "[k6] starting ${DISPLAY_NAME} run..."
echo "[k6] BASE_URL=${BASE_URL}"

export K6_BASE_URL="$BASE_URL"
docker compose -f docker/monitoring-compose.yml --profile "$PROFILE" run --rm k6 run -o experimental-prometheus-rw "$SCRIPT_PATH"
