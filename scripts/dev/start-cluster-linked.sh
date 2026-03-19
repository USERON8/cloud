#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/lib/runtime.sh"

ALL_SERVICES=(
  "auth-service|PORT_AUTH_SERVICE_HOST|28081|services/auth-service/target/auth-service-1.1.0.jar"
  "user-service|PORT_USER_SERVICE_HOST|28082|services/user-service/target/user-service-1.1.0.jar"
  "product-service|PORT_PRODUCT_SERVICE_HOST|28084|services/product-service/target/product-service-1.1.0.jar"
  "stock-service|PORT_STOCK_SERVICE_HOST|28085|services/stock-service/target/stock-service-1.1.0.jar"
  "payment-service|PORT_PAYMENT_SERVICE_HOST|28086|services/payment-service/target/payment-service-1.1.0.jar"
  "order-service|PORT_ORDER_SERVICE_HOST|28083|services/order-service/target/order-service-1.1.0.jar"
  "search-service|PORT_SEARCH_SERVICE_HOST|28087|services/search-service/target/search-service-1.1.0.jar"
  "gateway|PORT_GATEWAY_SERVICE_HOST|28080|services/gateway/target/gateway-1.1.0.jar"
)

requested_services=""
keep_host_services=0
dry_run=0
for arg in "$@"; do
  case "$arg" in
    --services=*) requested_services="${arg#*=}" ;;
    --keep-host-services) keep_host_services=1 ;;
    --dry-run) dry_run=1 ;;
  esac
done

assert_docker_daemon_ready() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "docker command not found, start Docker Desktop first" >&2
    exit 1
  fi
  if ! docker version >/dev/null 2>&1; then
    echo "Docker daemon is not ready. Start Docker Desktop first." >&2
    exit 1
  fi
}

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
      IFS='|' read -r svc_name _ _ _ <<< "$svc"
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
    selected+=("gateway|PORT_GATEWAY_SERVICE_HOST|28080|services/gateway/target/gateway-1.1.0.jar")
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
    IFS='|' read -r name _ _ _ <<< "$service"
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

service_module_path() {
  local jar_relative_path="$1"
  dirname "$(dirname "$jar_relative_path")"
}

ensure_service_artifacts() {
  local missing_names=()
  local module_paths=()
  local svc name jar module_path modules_csv

  for svc in "$@"; do
    IFS='|' read -r name _ _ jar <<< "$svc"
    if [ -f "$ROOT_DIR/$jar" ]; then
      continue
    fi
    missing_names+=("$name")
    module_path="$(service_module_path "$jar")"
    module_paths+=("$module_path")
  done

  if [ "${#missing_names[@]}" -eq 0 ]; then
    return 0
  fi

  if ! command -v mvn >/dev/null 2>&1; then
    echo "mvn not found, cannot build missing service artifacts" >&2
    exit 1
  fi

  modules_csv="$(printf "%s\n" "${module_paths[@]}" | awk '!seen[$0]++' | paste -sd, -)"
  echo "ARTIFACT_BUILD action=package services=$(IFS=,; echo "${missing_names[*]}") modules=${modules_csv}"
  (
    cd "$ROOT_DIR"
    mvn -DskipTests -T 1C -pl "$modules_csv" -am package
  )

  for svc in "$@"; do
    IFS='|' read -r name _ _ jar <<< "$svc"
    if [ ! -f "$ROOT_DIR/$jar" ]; then
      echo "service artifacts still missing after package: $name" >&2
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
  if grep -Eqi '<title>[[:space:]]*Please sign in[[:space:]]*</title>|action="/login"' "$body_file"; then
    rm -f "$body_file"
    echo "UP_SECURED"
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

cluster_startup_timeout_seconds="${CLUSTER_STARTUP_TIMEOUT_SECONDS:-900}"
case "$cluster_startup_timeout_seconds" in
  ''|*[!0-9]*)
    cluster_startup_timeout_seconds=300
    ;;
esac
if [ "$cluster_startup_timeout_seconds" -le 0 ]; then
  cluster_startup_timeout_seconds=300
fi

mapfile -t SERVICES < <(resolve_services "$requested_services")
service_csv=""
for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name _ _ _ <<< "$svc"
  if [ -n "$service_csv" ]; then
    service_csv="${service_csv},${name}"
  else
    service_csv="$name"
  fi
done
echo "SERVICE_SCOPE services=$service_csv"
assert_host_acceptance "${SERVICES[@]}"

if [ "$dry_run" = "1" ]; then
  if [ "$keep_host_services" != "1" ]; then
    bash "$SCRIPT_DIR/stop-services.sh" "--dry-run" "--services=${service_csv}"
  fi
  echo "DRY_RUN_DONE script=start-cluster-linked services=${service_csv} keepHostServices=${keep_host_services}"
  exit 0
fi

assert_docker_daemon_ready
ensure_service_artifacts "${SERVICES[@]}"

if [ "$keep_host_services" != "1" ]; then
  bash "$SCRIPT_DIR/stop-services.sh" "--services=${service_csv}"
  sleep 5
fi

service_names=()
for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name host_var host_port _ <<< "$svc"
  export "$host_var"="$host_port"
  service_names+=("$name")
done
export NGINX_GATEWAY_UPSTREAM="gateway:8080"

(
  cd "$ROOT_DIR/docker"
  docker compose -f docker-compose.yml --profile services up -d --pull missing --force-recreate nginx "${service_names[@]}"
)

declare -A READY_STATUS=()
for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name _ host_port _ <<< "$svc"
  echo "WAIT_CLUSTER service=$name port=$host_port status=waiting"
done

deadline=$(( $(date +%s) + cluster_startup_timeout_seconds ))
while [ "$(date +%s)" -lt "$deadline" ]; do
  pending_count=0
  for svc in "${SERVICES[@]}"; do
    IFS='|' read -r name _ host_port _ <<< "$svc"
    if [ -n "${READY_STATUS[$name]:-}" ]; then
      continue
    fi
    pending_count=$((pending_count + 1))
    if healthy="$(health_state "$host_port")"; then
      READY_STATUS["$name"]="$healthy"
      echo "WAIT_CLUSTER service=$name port=$host_port status=$healthy"
    fi
  done

  if [ "$pending_count" -eq 0 ]; then
    break
  fi
  sleep 2
done

pending_services=()
for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name _ host_port _ <<< "$svc"
  if [ -z "${READY_STATUS[$name]:-}" ]; then
    pending_services+=("${name}:${host_port}")
  fi
done
if [ "${#pending_services[@]}" -gt 0 ]; then
  echo "cluster service health not ready: ${pending_services[*]}" >&2
  exit 1
fi

nginx_port="$(docker_port_value "$ROOT_DIR" PORT_NGINX_HTTP 18080)"
echo "WAIT_CLUSTER service=nginx port=$nginx_port status=waiting"
nginx_healthy=""
deadline=$(( $(date +%s) + cluster_startup_timeout_seconds ))
while [ "$(date +%s)" -lt "$deadline" ]; do
  if nginx_healthy="$(health_state "$nginx_port")"; then
    break
  fi
  sleep 2
done
if [ -z "$nginx_healthy" ]; then
  echo "nginx gateway health not ready: $nginx_port" >&2
  exit 1
fi
echo "WAIT_CLUSTER service=nginx port=$nginx_port status=$nginx_healthy"

echo "CLUSTER_LINKED_READY"
