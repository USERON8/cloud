#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

check_http_up() {
  local name="$1"
  local url="$2"
  local timeout="${3:-10}"
  local end=$((SECONDS + timeout))
  local body_file header_file http_code
  body_file="$(mktemp)"
  header_file="$(mktemp)"
  trap 'rm -f "$body_file" "$header_file"' RETURN

  while [ "$SECONDS" -lt "$end" ]; do
    http_code="$(curl -sS --max-time 5 -o "$body_file" -D "$header_file" -w "%{http_code}" "$url" || true)"
    if grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' "$body_file"; then
      echo "SMOKE_OK ${name} ${url} status=UP"
      return 0
    fi
    if [[ "$http_code" =~ ^(301|302|303|307|308|401|403)$ ]] || grep -Eqi '<title>[[:space:]]*Please sign in[[:space:]]*</title>|action="/login"' "$body_file"; then
      echo "SMOKE_OK ${name} ${url} status=UP_SECURED http=${http_code}"
      return 0
    fi
    sleep 1
  done

  echo "SMOKE_FAIL ${name} ${url} http=${http_code}" >&2
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
