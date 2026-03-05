#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

mkdir -p "${ROOT_DIR}/.tmp/acceptance"

# Capture current process snapshot for rollback.
if [ -f "${ROOT_DIR}/.tmp/acceptance/pids.txt" ]; then
  cp "${ROOT_DIR}/.tmp/acceptance/pids.txt" "${ROOT_DIR}/.tmp/acceptance/pids.before-deploy.txt" || true
fi

echo "Deploy target: local node"
echo "Step 1/2: start infrastructure containers"
bash "${ROOT_DIR}/scripts/dev/start-containers.sh"

echo "Step 2/2: start backend services"
bash "${ROOT_DIR}/scripts/dev/start-services.sh"

echo "Local deploy completed."
