#!/usr/bin/env sh
set -eu

BASE_URL="${BASE_URL:-${K6_BASE_URL:-http://host.docker.internal:18080}}"
PROFILE="${PROFILE:-loadtest}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

cd "$ROOT_DIR"
. "$SCRIPT_DIR/lib/preflight.sh"
k6_preflight "all"

echo "[k6] starting acceptance load run..."
echo "[k6] BASE_URL=$BASE_URL"

K6_BASE_URL="$BASE_URL" docker compose -f docker/monitoring-compose.yml --profile "$PROFILE" run --rm k6
