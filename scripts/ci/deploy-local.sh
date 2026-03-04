#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "Deploy target: local node"
echo "Step 1/2: start infrastructure containers"
bash "${ROOT_DIR}/scripts/dev/start-containers.sh"

echo "Step 2/2: start backend services"
bash "${ROOT_DIR}/scripts/dev/start-services.sh"

echo "Local deploy completed."
