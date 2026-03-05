#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

check_http_up() {
  local name="$1"
  local url="$2"
  local timeout="${3:-10}"
  local end=$((SECONDS + timeout))
  while [ "$SECONDS" -lt "$end" ]; do
    if curl -fsS "$url" 2>/dev/null | grep -q "\"status\"[[:space:]]*:[[:space:]]*\"UP\""; then
      echo "SMOKE_OK ${name} ${url}"
      return 0
    fi
    sleep 1
  done
  echo "SMOKE_FAIL ${name} ${url}" >&2
  return 1
}

echo "Smoke: verify backend services"
check_http_up gateway "http://127.0.0.1:8080/actuator/health" 20
check_http_up auth-service "http://127.0.0.1:8081/actuator/health" 20
check_http_up user-service "http://127.0.0.1:8082/actuator/health" 20
check_http_up order-service "http://127.0.0.1:8083/actuator/health" 20
check_http_up product-service "http://127.0.0.1:8084/actuator/health" 20
check_http_up stock-service "http://127.0.0.1:8085/actuator/health" 20
check_http_up payment-service "http://127.0.0.1:8086/actuator/health" 20
check_http_up search-service "http://127.0.0.1:8087/actuator/health" 20

echo "Smoke: verify docker core containers"
docker inspect --format='{{.State.Status}}' cloud-mysql | grep -q running
docker inspect --format='{{.State.Status}}' cloud-redis | grep -q running
docker inspect --format='{{.State.Status}}' cloud-nacos | grep -q running
docker inspect --format='{{.State.Status}}' cloud-rmq-namesrv | grep -q running
docker inspect --format='{{.State.Status}}' cloud-rmq-broker | grep -q running

echo "SMOKE_ALL_OK"
