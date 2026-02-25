#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/lib/port-guard.sh"

import_dotenv() {
  local file="$1"
  [ -f "$file" ] || return 0
  while IFS= read -r line; do
    line="$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    [ -z "$line" ] && continue
    [[ "$line" == \#* ]] && continue
    if [[ "$line" == *=* ]]; then
      key="${line%%=*}"
      value="${line#*=}"
      if [ -z "${!key:-}" ]; then
        export "$key=$value"
      fi
    fi
  done < "$file"
}

import_dotenv "$ROOT_DIR/docker/.env"
export NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-${NACOS_HOST:-127.0.0.1}:${NACOS_PORT:-18848}}"
export ROCKETMQ_NAME_SERVER="${ROCKETMQ_NAME_SERVER:-${ROCKETMQ_NAMESRV_HOST:-127.0.0.1}:${ROCKETMQ_NAMESRV_PORT:-19876}}"
export AUTH_HOST="${AUTH_HOST:-127.0.0.1}"
export AUTH_PORT="${AUTH_PORT:-8081}"
export AUTH_ISSUER_URI="${AUTH_ISSUER_URI:-http://${AUTH_HOST}:${AUTH_PORT}}"
export AUTH_JWK_SET_URI="${AUTH_JWK_SET_URI:-http://${AUTH_HOST}:${AUTH_PORT}/.well-known/jwks.json}"
export AUTH_TOKEN_URI="${AUTH_TOKEN_URI:-http://${AUTH_HOST}:${AUTH_PORT}/oauth2/token}"
export DB_HOST="${DB_HOST:-127.0.0.1}"
export DB_PORT="${DB_PORT:-${PORT_MYSQL:-13306}}"
export REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
export REDIS_PORT="${REDIS_PORT:-${PORT_REDIS:-16379}}"
export ELASTICSEARCH_URIS="${ELASTICSEARCH_URIS:-http://127.0.0.1:${PORT_ES_HTTP:-19200}}"
export MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://127.0.0.1:${PORT_MINIO_API:-19000}}"
export MINIO_PUBLIC_ENDPOINT="${MINIO_PUBLIC_ENDPOINT:-$MINIO_ENDPOINT}"

KILL_PORTS=1
DRY_RUN=0
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    --kill-ports) KILL_PORTS=1 ;;
    --no-kill-ports) KILL_PORTS=0 ;;
  esac
done

SERVICE_PORTS=(8080 8081 8082 8083 8084 8085 8086 8087)
if [ "$KILL_PORTS" = "1" ]; then
  for p in "${SERVICE_PORTS[@]}"; do
    kill_port_owner "$p" "$DRY_RUN"
  done
fi
if [ "$DRY_RUN" = "1" ]; then
  echo "DRY_RUN_DONE script=start-services"
  exit 0
fi

JAVA_BIN="${JAVA_HOME:-}/bin/java"
if [ -x "$JAVA_BIN" ]; then
  JAVA_CMD="$JAVA_BIN"
else
  JAVA_CMD="java"
fi

LOG_DIR="$ROOT_DIR/.tmp/acceptance/logs"
mkdir -p "$LOG_DIR"

SERVICES=(
  "gateway|8080|gateway/target/gateway-0.0.1-SNAPSHOT.jar|dev,route"
  "auth-service|8081|auth-service/target/auth-service-0.0.1-SNAPSHOT.jar|dev"
  "user-service|8082|user-service/target/user-service-0.0.1-SNAPSHOT.jar|dev"
  "order-service|8083|order-service/target/order-service-0.0.1-SNAPSHOT.jar|dev"
  "product-service|8084|product-service/target/product-service-0.0.1-SNAPSHOT.jar|dev"
  "stock-service|8085|stock-service/target/stock-service-0.0.1-SNAPSHOT.jar|dev"
  "payment-service|8086|payment-service/target/payment-service-0.0.1-SNAPSHOT.jar|dev"
  "search-service|8087|search-service/target/search-service-0.0.1-SNAPSHOT.jar|dev"
)

RESULT_ROWS=()
ALL_OK=1

health_status() {
  local port="$1"
  curl -fsS "http://127.0.0.1:${port}/actuator/health" 2>/dev/null | grep -q "\"status\"[[:space:]]*:[[:space:]]*\"UP\""
}

for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name port jar profiles <<< "$svc"
  jar_path="$ROOT_DIR/$jar"
  if [ ! -f "$jar_path" ]; then
    echo "jar missing: $jar_path" >&2
    exit 1
  fi

  out_log="$LOG_DIR/${name}.out.log"
  err_log="$LOG_DIR/${name}.err.log"
  : > "$out_log"
  : > "$err_log"

  start_ts="$(date +%s)"
  nohup "$JAVA_CMD" -jar "$jar_path" --spring.profiles.active="$profiles" >"$out_log" 2>"$err_log" &
  pid=$!

  status="TIMEOUT"
  healthy=0
  deadline=$((start_ts + 180))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      status="EXITED"
      break
    fi
    if health_status "$port"; then
      status="UP"
      healthy=1
      break
    fi
    sleep 2
  done

  end_ts="$(date +%s)"
  duration=$((end_ts - start_ts))
  RESULT_ROWS+=("${name},${port},${pid},${status},${duration},${out_log},${err_log}")
  echo "SERVICE_START service=${name} port=${port} pid=${pid} health=${status}"

  if [ "$healthy" != "1" ]; then
    ALL_OK=0
    break
  fi
done

mkdir -p "$ROOT_DIR/.tmp/acceptance"
{
  echo "service,port,pid,status,startup_seconds,out_log,err_log"
  for row in "${RESULT_ROWS[@]}"; do
    echo "$row"
  done
} > "$ROOT_DIR/.tmp/acceptance/startup.csv"

{
  for row in "${RESULT_ROWS[@]}"; do
    IFS=',' read -r s p pid st _ <<< "$row"
    echo "${s},${p},${pid},${st}"
  done
} > "$ROOT_DIR/.tmp/acceptance/pids.txt"

if [ "$ALL_OK" = "1" ]; then
  echo "STARTUP_OK"
else
  echo "STARTUP_FAILED"
fi
