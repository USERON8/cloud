#!/usr/bin/env sh
set -eu

BASE_URL="${1:-http://host.docker.internal:18080}"
PROFILE="${2:-loadtest}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

exec "$SCRIPT_DIR/run-k6.sh" search-chain "$BASE_URL" "$PROFILE"
