#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/lib/runtime.sh"

ALL_SERVICES=(
  "gateway|PORT_GATEWAY_SERVICE_HOST|28080"
  "auth-service|PORT_AUTH_SERVICE_HOST|28081"
  "user-service|PORT_USER_SERVICE_HOST|28082"
  "order-service|PORT_ORDER_SERVICE_HOST|28083"
  "product-service|PORT_PRODUCT_SERVICE_HOST|28084"
  "stock-service|PORT_STOCK_SERVICE_HOST|28085"
  "payment-service|PORT_PAYMENT_SERVICE_HOST|28086"
  "search-service|PORT_SEARCH_SERVICE_HOST|28087"
)

requested_services=""
for arg in "$@"; do
  case "$arg" in
    --services=*) requested_services="${arg#*=}" ;;
  esac
done

resolve_services() {
  local requested="$1"
  if [ -z "$requested" ]; then
    printf '%s\n' "${ALL_SERVICES[@]}"
    return 0
  fi

  local selected=()
  local seen=","
  local found_gateway=0
  IFS=',' read -r -a requested_items <<< "$requested"
  for raw in "${requested_items[@]}"; do
    local name="${raw#"${raw%%[![:space:]]*}"}"
    name="${name%"${name##*[![:space:]]}"}"
    [ -n "$name" ] || continue
    [[ "$seen" == *",$name,"* ]] && continue
    seen="${seen}${name},"
    local matched=""
    for svc in "${ALL_SERVICES[@]}"; do
      IFS='|' read -r svc_name _ _ <<< "$svc"
      if [ "$svc_name" = "$name" ]; then
        matched="$svc"
        [ "$svc_name" = "gateway" ] && found_gateway=1
        break
      fi
    done
    [ -n "$matched" ] || {
      echo "unknown service: $name" >&2
      exit 1
    }
    selected+=("$matched")
  done
  if [ "$found_gateway" != "1" ]; then
    selected=("gateway|PORT_GATEWAY_SERVICE_HOST|28080" "${selected[@]}")
  fi
  printf '%s\n' "${selected[@]}"
}

assert_host_acceptance() {
  local startup_file="$ROOT_DIR/.tmp/acceptance/startup.csv"
  [ -f "$startup_file" ] || {
    echo "host acceptance not found, run start-host-linked first" >&2
    exit 1
  }

  local service
  for service in "$@"; do
    IFS='|' read -r name _ _ <<< "$service"
    local row
    row="$(awk -F, -v svc="$name" 'NR > 1 && $1 == svc { print $0; exit }' "$startup_file")"
    [ -n "$row" ] || {
      echo "host acceptance missing service: $name" >&2
      exit 1
    }
    IFS=',' read -r _ _ _ status _ _ _ <<< "$row"
    if [[ "$status" != "UP" && "$status" != "UP_SECURED" ]]; then
      echo "host acceptance failed for service: $name status=$status" >&2
      exit 1
    fi
  done
}

health_state() {
  local port="$1"
  local body_file http_code
  body_file="$(mktemp)"
  http_code="$(curl --noproxy '*' -sS --max-time 5 -o "$body_file" -w "%{http_code}" "http://127.0.0.1:${port}/actuator/health" || true)"
  if grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' "$body_file"; then
    rm -f "$body_file"
    echo "UP"
    return 0
  fi
  if [[ "$http_code" =~ ^(301|302|303|307|308|401|403)$ ]]; then
    rm -f "$body_file"
    echo "UP_SECURED"
    return 0
  fi
  rm -f "$body_file"
  return 1
}

mapfile -t SERVICES < <(resolve_services "$requested_services")
assert_host_acceptance "${SERVICES[@]}"

service_names=()
for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name host_var host_port <<< "$svc"
  export "$host_var"="$host_port"
  service_names+=("$name")
done
export NGINX_GATEWAY_UPSTREAM="gateway:8080"

(
  cd "$ROOT_DIR/docker"
  docker compose -f docker-compose.yml --profile services up -d --pull never --force-recreate nginx "${service_names[@]}"
)

for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name _ host_port <<< "$svc"
  echo "WAIT_CLUSTER service=$name port=$host_port status=waiting"
  healthy=""
  deadline=$(( $(date +%s) + 120 ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    if healthy="$(health_state "$host_port")"; then
      break
    fi
    sleep 2
  done
  if [ -z "$healthy" ]; then
    echo "cluster service health not ready: $name:$host_port" >&2
    exit 1
  fi
  echo "WAIT_CLUSTER service=$name port=$host_port status=$healthy"
done

nginx_port="$(docker_port_value "$ROOT_DIR" PORT_NGINX_HTTP 18080)"
if ! wait_tcp_port "127.0.0.1" "$nginx_port" 60; then
  echo "nginx http port not ready after cluster deployment" >&2
  exit 1
fi

echo "CLUSTER_LINKED_READY"
