#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/lib/port-guard.sh"
source "$SCRIPT_DIR/lib/runtime.sh"

trim_value() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

resolve_writable_dir() {
  local preferred_dir="$1"
  local fallback_dir="$2"

  if mkdir -p "$preferred_dir" >/dev/null 2>&1 && [ -w "$preferred_dir" ]; then
    printf '%s\n' "$preferred_dir"
    return 0
  fi

  mkdir -p "$fallback_dir"
  printf '%s\n' "$fallback_dir"
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
    IFS='|' read -r name _ jar _ <<< "$svc"
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
    IFS='|' read -r name _ jar _ <<< "$svc"
    if [ ! -f "$ROOT_DIR/$jar" ]; then
      echo "service artifacts still missing after package: $name" >&2
      exit 1
    fi
  done
}

service_environment_overrides() {
  local service_name="$1"
  case "$service_name" in
    user-service)
      printf '%s\n' "DUBBO_PROTOCOL_PORT=20880"
      printf '%s\n' "APP_MYBATIS_ILLEGAL_SQL_ENABLED=false"
      ;;
    order-service)
      printf '%s\n' "DUBBO_PROTOCOL_PORT=20882"
      ;;
    product-service)
      printf '%s\n' "DUBBO_PROTOCOL_PORT=20884"
      ;;
    stock-service)
      printf '%s\n' "DUBBO_PROTOCOL_PORT=20886"
      ;;
    payment-service)
      printf '%s\n' "DUBBO_PROTOCOL_PORT=20888"
      ;;
    auth-service|search-service)
      if [ "$service_name" = "search-service" ]; then
        printf '%s\n' "DUBBO_PROTOCOL_PORT=20890"
      fi
      ;;
  esac
}

KILL_PORTS=1
DRY_RUN=0
SERVICES_FILTER=""
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    --kill-ports) KILL_PORTS=1 ;;
    --no-kill-ports) KILL_PORTS=0 ;;
    --services=*) SERVICES_FILTER="${arg#*=}" ;;
  esac
done

ALL_SERVICES=(
  "user-service|8082|services/user-service/target/user-service-1.1.0.jar|dev"
  "auth-service|8081|services/auth-service/target/auth-service-1.1.0.jar|dev"
  "product-service|8084|services/product-service/target/product-service-1.1.0.jar|dev"
  "stock-service|8085|services/stock-service/target/stock-service-1.1.0.jar|dev"
  "payment-service|8086|services/payment-service/target/payment-service-1.1.0.jar|dev"
  "order-service|8083|services/order-service/target/order-service-1.1.0.jar|dev"
  "search-service|8087|services/search-service/target/search-service-1.1.0.jar|dev"
  "gateway|8080|services/gateway/target/gateway-1.1.0.jar|dev,route"
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
  if [ "${#SERVICES[@]}" -eq 0 ]; then
    echo "No services selected. Use --services=name1,name2." >&2
    exit 1
  fi
fi

echo "SERVICE_SCOPE services=$(IFS=,; echo "${SELECTED_SERVICE_NAMES[*]}")"

SERVICE_PORTS=()
for svc in "${SERVICES[@]}"; do
  IFS='|' read -r _ port _ <<< "$svc"
  SERVICE_PORTS+=("$port")
done
if [ "$KILL_PORTS" = "1" ]; then
  for port in "${SERVICE_PORTS[@]}"; do
    kill_port_owner "$port" "$DRY_RUN"
  done
fi
if [ "$DRY_RUN" = "1" ]; then
  echo "DRY_RUN_DONE script=start-services"
  exit 0
fi

ensure_service_artifacts "${SERVICES[@]}"
export_service_runtime_env "$ROOT_DIR"

JAVA_BIN="${JAVA_HOME:-}/bin/java"
if [ -x "$JAVA_BIN" ]; then
  JAVA_CMD="$JAVA_BIN"
else
  JAVA_CMD="java"
fi

RUNTIME_LOG_ROOT="${SERVICE_RUNTIME_LOG_ROOT:-$ROOT_DIR/.tmp/service-runtime}"
mkdir -p "$RUNTIME_LOG_ROOT"

SKYWALKING_AGENT_DIR="$ROOT_DIR/docker/monitor/skywalking/agent"
SKYWALKING_AGENT_JAR="$SKYWALKING_AGENT_DIR/skywalking-agent.jar"
SKYWALKING_AGENT_AVAILABLE=0
case "${SKYWALKING_ENABLED:-true}" in
  false|0|no|off)
    echo "SKYWALKING disabled reason=env-disabled" >&2
    ;;
  *)
    if [ -f "$SKYWALKING_AGENT_JAR" ]; then
      SKYWALKING_AGENT_AVAILABLE=1
    else
      echo "SKYWALKING disabled reason=agent-not-found path=$SKYWALKING_AGENT_JAR" >&2
    fi
    ;;
esac
SERVICE_JVM_OPTS="${SERVICE_JVM_OPTS:--Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError}"
if [ -n "${NACOS_GRPC_PORT_OFFSET:-}" ] && [[ "$SERVICE_JVM_OPTS" != *"nacos.server.grpc.port.offset="* ]]; then
  SERVICE_JVM_OPTS="$SERVICE_JVM_OPTS -Dnacos.server.grpc.port.offset=${NACOS_GRPC_PORT_OFFSET}"
fi
SERVICE_STARTUP_TIMEOUT_SECONDS="${SERVICE_STARTUP_TIMEOUT_SECONDS:-300}"
SERVICE_HEALTH_STABILIZATION_SECONDS="${SERVICE_HEALTH_STABILIZATION_SECONDS:-8}"

RESULT_ROWS=()
ALL_OK=1

health_status() {
  local port="$1"
  local body_file header_file http_code
  body_file="$(mktemp)"
  header_file="$(mktemp)"
  http_code="$(curl --noproxy '*' -sS --max-time 5 -o "$body_file" -D "$header_file" -w "%{http_code}" "http://127.0.0.1:${port}/actuator/health" || true)"

  if grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' "$body_file"; then
    rm -f "$body_file" "$header_file"
    echo "UP"
    return 0
  fi

  if [[ "$http_code" =~ ^(301|302|303|307|308|401|403)$ ]] || grep -Eqi '<title>[[:space:]]*Please sign in[[:space:]]*</title>|action="/login"' "$body_file"; then
    rm -f "$body_file" "$header_file"
    echo "UP_SECURED"
    return 0
  fi

  rm -f "$body_file" "$header_file"
  return 1
}

write_state_files() {
  local acceptance_dir startup_path pids_path row service port pid status startup_seconds out_log err_log name
  acceptance_dir="$ROOT_DIR/.tmp/acceptance"
  startup_path="$acceptance_dir/startup.csv"
  pids_path="$acceptance_dir/pids.txt"
  mkdir -p "$acceptance_dir"

  declare -A STARTUP_ROWS=()
  if [ -f "$startup_path" ]; then
    while IFS=',' read -r service port pid status startup_seconds out_log err_log; do
      if [ "$service" = "service" ] || [ -z "$service" ]; then
        continue
      fi
      STARTUP_ROWS["$service"]="$service,$port,$pid,$status,$startup_seconds,$out_log,$err_log"
    done < "$startup_path"
  fi

  declare -A PID_ROWS=()
  if [ -f "$pids_path" ]; then
    while IFS=',' read -r service port pid status; do
      if [ -z "$service" ]; then
        continue
      fi
      PID_ROWS["$service"]="$service,$port,$pid,$status"
    done < "$pids_path"
  fi

  for row in "${RESULT_ROWS[@]}"; do
    IFS=',' read -r service port pid status startup_seconds out_log err_log <<< "$row"
    STARTUP_ROWS["$service"]="$row"
    PID_ROWS["$service"]="$service,$port,$pid,$status"
  done

  {
    echo "service,port,pid,status,startup_seconds,out_log,err_log"
    for name in "${ALL_SERVICE_NAMES[@]}"; do
      if [ -n "${STARTUP_ROWS[$name]:-}" ]; then
        echo "${STARTUP_ROWS[$name]}"
      fi
    done
  } > "$startup_path"

  {
    for name in "${ALL_SERVICE_NAMES[@]}"; do
      if [ -n "${PID_ROWS[$name]:-}" ]; then
        echo "${PID_ROWS[$name]}"
      fi
    done
  } > "$pids_path"
}

for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name port jar profiles <<< "$svc"
  jar_path="$ROOT_DIR/$jar"
  if [ ! -f "$jar_path" ]; then
    echo "jar missing: $jar_path" >&2
    exit 1
  fi

  service_dir="$(cd "$(dirname "$jar_path")/.." && pwd)"
  runtime_log_dir="$RUNTIME_LOG_ROOT/$name"
  mkdir -p "$runtime_log_dir"
  service_log_dir="$(resolve_writable_dir "$service_dir/logs" "$runtime_log_dir/app-logs")"
  if [ "$service_log_dir" != "$service_dir/logs" ]; then
    echo "LOG_DIR_FALLBACK service=$name path=$service_log_dir"
  fi

  out_log="$runtime_log_dir/stdout.log"
  err_log="$runtime_log_dir/stderr.log"
  : > "$out_log"
  : > "$err_log"

  java_args=()
  if [ -n "$SERVICE_JVM_OPTS" ]; then
    read -r -a jvm_opts_array <<< "$SERVICE_JVM_OPTS"
    java_args+=("${jvm_opts_array[@]}")
  fi
  if [[ "$SERVICE_JVM_OPTS" != *HeapDumpPath* ]]; then
    java_args+=("-XX:HeapDumpPath=${service_log_dir}/heap.hprof")
  fi
  java_args+=(
    "-jar" "$jar_path"
    "--spring.profiles.active=$profiles"
    "--logging.file.path=$service_log_dir"
  )

  env_args=()
  if [ "$SKYWALKING_AGENT_AVAILABLE" = "1" ]; then
    skywalking_log_dir="$runtime_log_dir/skywalking-agent"
    mkdir -p "$skywalking_log_dir"
    java_tool_opts="-javaagent:${SKYWALKING_AGENT_JAR}"
    if [ -n "${JAVA_TOOL_OPTIONS:-}" ]; then
      java_tool_opts="${JAVA_TOOL_OPTIONS} ${java_tool_opts}"
    fi
    env_args+=("JAVA_TOOL_OPTIONS=${java_tool_opts}" "SW_AGENT_NAME=${name}" "SW_LOGGING_DIR=${skywalking_log_dir}")
  fi
  while IFS= read -r override; do
    [ -n "$override" ] || continue
    env_args+=("$override")
  done < <(service_environment_overrides "$name")

  start_ts="$(date +%s)"
  if command -v setsid >/dev/null 2>&1; then
    if [ "${#env_args[@]}" -gt 0 ]; then
      nohup setsid env "${env_args[@]}" "$JAVA_CMD" "${java_args[@]}" >"$out_log" 2>"$err_log" < /dev/null &
    else
      nohup setsid "$JAVA_CMD" "${java_args[@]}" >"$out_log" 2>"$err_log" < /dev/null &
    fi
  else
    if [ "${#env_args[@]}" -gt 0 ]; then
      nohup env "${env_args[@]}" "$JAVA_CMD" "${java_args[@]}" >"$out_log" 2>"$err_log" < /dev/null &
    else
      nohup "$JAVA_CMD" "${java_args[@]}" >"$out_log" 2>"$err_log" < /dev/null &
    fi
  fi
  pid=$!

  status="TIMEOUT"
  healthy=0
  deadline=$((start_ts + SERVICE_STARTUP_TIMEOUT_SECONDS))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      status="EXITED"
      break
    fi
    if health_state="$(health_status "$port")"; then
      stabilized=1
      if [ "$SERVICE_HEALTH_STABILIZATION_SECONDS" -gt 0 ]; then
        stabilization_deadline=$(( $(date +%s) + SERVICE_HEALTH_STABILIZATION_SECONDS ))
        while [ "$(date +%s)" -lt "$stabilization_deadline" ]; do
          if ! kill -0 "$pid" >/dev/null 2>&1; then
            stabilized=0
            break
          fi
          if ! health_status "$port" >/dev/null; then
            stabilized=0
            break
          fi
          sleep 2
        done
      fi
      if [ "$stabilized" = "1" ]; then
        status="$health_state"
        healthy=1
        break
      fi
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

write_state_files

if [ "$ALL_OK" = "1" ]; then
  echo "STARTUP_OK"
else
  echo "STARTUP_FAILED"
  exit 1
fi
