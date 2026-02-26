#!/usr/bin/env sh
set -eu

BASE_URL="${BASE_URL:-${K6_BASE_URL:-http://host.docker.internal:18080}}"
PROFILE="${PROFILE:-loadtest}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

exec "$SCRIPT_DIR/run-k6.sh" acceptance "$BASE_URL" "$PROFILE"
