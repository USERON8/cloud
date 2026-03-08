#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/lib/port-guard.sh"

trim_value() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

stop_pid_gracefully() {
  local pid="$1"
  local timeout_seconds="$2"
  local start_ts now

  if ! kill -0 "$pid" >/dev/null 2>&1; then
    return 0
  fi

  kill "$pid" >/dev/null 2>&1 || true
  start_ts="$(date +%s)"
  while kill -0 "$pid" >/dev/null 2>&1; do
    now="$(date +%s)"
    if [ $((now - start_ts)) -ge "$timeout_seconds" ]; then
      kill -9 "$pid" >/dev/null 2>&1 || true
      break
    fi
    sleep 1
  done

  for _ in $(seq 1 10); do
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.2
  done
  return 1
}

process_matches_service() {
  local pid="$1"
  local service_name="$2"
  local jar_path="$3"
  local cmd jar_name

  cmd="$(ps -p "$pid" -o args= 2>/dev/null || true)"
  jar_name="$(basename "$jar_path")"
  [ -n "$cmd" ] || return 1

  [[ "$cmd" == *"$jar_name"* ]] || [[ "$cmd" == *"$service_name"* ]]
}

DRY_RUN=0
SERVICES_FILTER=""
STOP_TIMEOUT_SECONDS="${SERVICE_STOP_TIMEOUT_SECONDS:-20}"
FORCE_PORT_CLEANUP=1

for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    --services=*) SERVICES_FILTER="${arg#*=}" ;;
    --timeout=*) STOP_TIMEOUT_SECONDS="${arg#*=}" ;;
    --force-ports) FORCE_PORT_CLEANUP=1 ;;
    --no-force-ports) FORCE_PORT_CLEANUP=0 ;;
  esac
done

if ! [[ "$STOP_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]]; then
  echo "Invalid timeout: $STOP_TIMEOUT_SECONDS" >&2
  exit 1
fi

ALL_SERVICES=(
  "gateway|8080|services/gateway/target/gateway-0.0.1-SNAPSHOT.jar"
  "auth-service|8081|services/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar"
  "user-service|8082|services/user-service/target/user-service-0.0.1-SNAPSHOT.jar"
  "order-service|8083|services/order-service/target/order-service-0.0.1-SNAPSHOT.jar"
  "product-service|8084|services/product-service/target/product-service-0.0.1-SNAPSHOT.jar"
  "stock-service|8085|services/stock-service/target/stock-service-0.0.1-SNAPSHOT.jar"
  "payment-service|8086|services/payment-service/target/payment-service-0.0.1-SNAPSHOT.jar"
  "search-service|8087|services/search-service/target/search-service-0.0.1-SNAPSHOT.jar"
)

declare -A SERVICE_INDEX=()
ALL_SERVICE_NAMES=()
for svc in "${ALL_SERVICES[@]}"; do
  IFS='|' read -r name _ <<< "$svc"
  SERVICE_INDEX["$name"]="$svc"
  ALL_SERVICE_NAMES+=("$name")
done

SERVICES=()
SELECTED_SERVICE_NAMES=()
if [ -z "$SERVICES_FILTER" ]; then
  SERVICES=("${ALL_SERVICES[@]}")
  SELECTED_SERVICE_NAMES=("${ALL_SERVICE_NAMES[@]}")
else
  declare -A SEEN_SERVICES=()
  MISSING_SERVICES=()
  IFS=',' read -r -a REQUESTED_SERVICES <<< "$SERVICES_FILTER"
  for raw_name in "${REQUESTED_SERVICES[@]}"; do
    service_name="$(trim_value "$raw_name")"
    if [ -z "$service_name" ] || [ -n "${SEEN_SERVICES[$service_name]:-}" ]; then
      continue
    fi
    SEEN_SERVICES["$service_name"]=1
    if [ -n "${SERVICE_INDEX[$service_name]:-}" ]; then
      SERVICES+=("${SERVICE_INDEX[$service_name]}")
      SELECTED_SERVICE_NAMES+=("$service_name")
    else
      MISSING_SERVICES+=("$service_name")
    fi
  done
  if [ "${#MISSING_SERVICES[@]}" -gt 0 ]; then
    echo "Unknown services: ${MISSING_SERVICES[*]}" >&2
    exit 1
  fi
fi

echo "SERVICE_SCOPE services=$(IFS=,; echo "${SELECTED_SERVICE_NAMES[*]}")"

PIDS_FILE="$ROOT_DIR/.tmp/acceptance/pids.txt"
STARTUP_FILE="$ROOT_DIR/.tmp/acceptance/startup.csv"

declare -A SELECTED_INDEX=()
for name in "${SELECTED_SERVICE_NAMES[@]}"; do
  SELECTED_INDEX["$name"]=1
done

declare -A PID_FILE_ROWS=()
if [ -f "$PIDS_FILE" ]; then
  while IFS=',' read -r service port pid status; do
    if [ -z "${service:-}" ]; then
      continue
    fi
    PID_FILE_ROWS["$service"]="$service,$port,$pid,$status"
  done < "$PIDS_FILE"
fi

filter_state_file() {
  local file_path="$1"
  local has_header="$2"
  local tmp_path service rest

  [ -f "$file_path" ] || return 0

  tmp_path="$(mktemp)"
  if [ "$has_header" = "1" ]; then
    echo "service,port,pid,status,startup_seconds,out_log,err_log" > "$tmp_path"
  fi

  while IFS=',' read -r service rest; do
    if [ -z "${service:-}" ]; then
      continue
    fi
    if [ "$service" = "service" ]; then
      continue
    fi
    if [ -z "${SELECTED_INDEX[$service]:-}" ]; then
      echo "$service,$rest" >> "$tmp_path"
    fi
  done < "$file_path"

  mv "$tmp_path" "$file_path"
}

declare -A PROCESSED_PIDS=()
STOP_FAILED=0

stop_candidate_pid() {
  local service_name="$1"
  local port="$2"
  local pid="$3"
  local source="$4"

  if [ -n "${PROCESSED_PIDS[$pid]:-}" ]; then
    return 0
  fi
  PROCESSED_PIDS["$pid"]=1

  if [ "$DRY_RUN" = "1" ]; then
    echo "SERVICE_STOP service=$service_name port=$port pid=$pid source=$source action=dry-run"
    return 0
  fi

  if ! kill -0 "$pid" >/dev/null 2>&1; then
    echo "SERVICE_STOP service=$service_name port=$port pid=$pid source=$source action=skip reason=not-running"
    return 0
  fi

  if stop_pid_gracefully "$pid" "$STOP_TIMEOUT_SECONDS"; then
    echo "SERVICE_STOP service=$service_name port=$port pid=$pid source=$source action=stopped"
    return 0
  fi

  echo "SERVICE_STOP service=$service_name port=$port pid=$pid source=$source action=failed" >&2
  return 1
}

for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name port jar <<< "$svc"
  jar_path="$ROOT_DIR/$jar"
  matched_pid=0

  if [ -n "${PID_FILE_ROWS[$name]:-}" ]; then
    IFS=',' read -r _ _ pid _ <<< "${PID_FILE_ROWS[$name]}"
    if [ -n "${pid:-}" ] && kill -0 "$pid" >/dev/null 2>&1; then
      if process_matches_service "$pid" "$name" "$jar_path"; then
        matched_pid=1
        if ! stop_candidate_pid "$name" "$port" "$pid" "pid-file"; then
          STOP_FAILED=1
        fi
      else
        echo "SERVICE_STOP service=$name port=$port pid=$pid source=pid-file action=skip reason=pid-mismatch"
      fi
    fi
  fi

  if [ "$FORCE_PORT_CLEANUP" = "1" ]; then
    port_lines="$(get_port_owner_lines "$port" || true)"
    if [ -n "$port_lines" ]; then
      while read -r owner_pid _; do
        [ -z "${owner_pid:-}" ] && continue
        matched_pid=1
        if ! stop_candidate_pid "$name" "$port" "$owner_pid" "port-owner"; then
          STOP_FAILED=1
        fi
      done <<< "$port_lines"
    fi
  fi

  if [ "$DRY_RUN" = "1" ]; then
    if [ "$matched_pid" = "0" ]; then
      echo "SERVICE_STOP service=$name port=$port pid=- action=dry-run reason=already-stopped"
    fi
    continue
  fi

  if get_port_owner_lines "$port" | grep -q .; then
    echo "SERVICE_STOP service=$name port=$port pid=- action=failed reason=port-occupied" >&2
    STOP_FAILED=1
  elif [ "$matched_pid" = "0" ]; then
    echo "SERVICE_STOP service=$name port=$port pid=- action=skip reason=already-stopped"
  fi
done

if [ "$DRY_RUN" = "0" ]; then
  filter_state_file "$PIDS_FILE" 0
  filter_state_file "$STARTUP_FILE" 1
fi

if [ "$STOP_FAILED" = "1" ]; then
  echo "STOP_FAILED"
  exit 1
fi

echo "STOP_OK"
