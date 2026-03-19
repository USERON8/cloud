#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/lib/runtime.sh"

WITH_MONITORING=0
NO_KILL_PORTS=0
SKIP_CONTAINERS=0
SKIP_SERVICES=0
SERVICES_FILTER=""
OPEN_DASHBOARDS=0
DRY_RUN=0

for arg in "$@"; do
  case "$arg" in
    --with-monitoring) WITH_MONITORING=1 ;;
    --no-kill-ports) NO_KILL_PORTS=1 ;;
    --skip-containers) SKIP_CONTAINERS=1 ;;
    --skip-services) SKIP_SERVICES=1 ;;
    --services=*) SERVICES_FILTER="${arg#*=}" ;;
    --open-dashboards) OPEN_DASHBOARDS=1 ;;
    --dry-run) DRY_RUN=1 ;;
  esac
done

invoke_step() {
  local script_path="$1"
  local error_message="$2"
  shift 2
  bash "$script_path" "$@"
  local exit_code=$?
  if [ "$exit_code" -ne 0 ]; then
    echo "$error_message" >&2
    exit "$exit_code"
  fi
}

wait_infrastructure() {
  local wait_monitoring="$1"
  local wait_skywalking="$2"
  local targets=(
    "mysql|$(docker_port_value "$ROOT_DIR" PORT_MYSQL 13306)"
    "redis|$(docker_port_value "$ROOT_DIR" PORT_REDIS 16379)"
    "nacos|$(docker_port_value "$ROOT_DIR" PORT_NACOS_HTTP 18848)"
    "nacos-grpc|$(docker_port_value "$ROOT_DIR" PORT_NACOS_GRPC 19848)"
    "nacos-grpc-compat|$(docker_port_value "$ROOT_DIR" PORT_NACOS_GRPC_COMPAT 12937)"
    "rocketmq-namesrv|$(docker_port_value "$ROOT_DIR" PORT_RMQ_NAMESRV 20011)"
    "elasticsearch|$(docker_port_value "$ROOT_DIR" PORT_ES_HTTP 19200)"
    "minio|$(docker_port_value "$ROOT_DIR" PORT_MINIO_API 19000)"
    "seata-server|$(docker_port_value "$ROOT_DIR" PORT_SEATA_SERVER 18091)"
  )

  if [ "$wait_skywalking" = "1" ]; then
    targets+=("skywalking-oap|$(docker_port_value "$ROOT_DIR" PORT_SKYWALKING_OAP_GRPC 11800)")
  fi
  if [ "$wait_monitoring" = "1" ]; then
    targets+=(
      "prometheus|$(docker_port_value "$ROOT_DIR" PORT_PROMETHEUS_HTTP 19099)"
      "grafana|$(docker_port_value "$ROOT_DIR" PORT_GRAFANA_HTTP 13000)"
    )
  fi

  nacos_health_ready() {
    local port="$1"
    local body
    body="$(curl --noproxy '*' -sS --max-time 5 "http://127.0.0.1:${port}/nacos/actuator/health" || true)"
    [[ "$body" =~ \"status\"[[:space:]]*:[[:space:]]*\"UP\" ]]
  }

  local target name port
  for target in "${targets[@]}"; do
    IFS='|' read -r name port <<< "$target"
    echo "WAIT_PORT name=$name port=$port status=waiting"
    if ! wait_tcp_port "127.0.0.1" "$port" 120; then
      echo "Infrastructure port not ready: $name:$port" >&2
      exit 1
    fi
    if [ "$name" = "nacos" ]; then
      local deadline=$(( $(date +%s) + 120 ))
      local ready=0
      while [ "$(date +%s)" -lt "$deadline" ]; do
        if nacos_health_ready "$port"; then
          ready=1
          break
        fi
        sleep 2
      done
      if [ "$ready" != "1" ]; then
        echo "Infrastructure service not ready: $name:$port" >&2
        exit 1
      fi
    fi
    echo "WAIT_PORT name=$name port=$port status=ready"
  done

  local nacos_ready_grace_seconds="${NACOS_READY_GRACE_SECONDS:-15}"
  case "$nacos_ready_grace_seconds" in
    ''|*[!0-9]*)
      nacos_ready_grace_seconds=15
      ;;
  esac
  if [ "$nacos_ready_grace_seconds" -gt 0 ]; then
    echo "WAIT_PORT name=nacos-ready grace=$nacos_ready_grace_seconds status=waiting"
    sleep "$nacos_ready_grace_seconds"
    echo "WAIT_PORT name=nacos-ready grace=$nacos_ready_grace_seconds status=ready"
  fi
}

echo "=== START PLATFORM ==="

container_args=()
if [ "$WITH_MONITORING" = "1" ]; then container_args+=("--with-monitoring"); fi
if [ "$NO_KILL_PORTS" = "1" ]; then container_args+=("--no-kill-ports"); fi
if [ "$DRY_RUN" = "1" ]; then container_args+=("--dry-run"); fi

if [ "$SKIP_CONTAINERS" = "0" ]; then
  echo "STEP 1/3 containers=start"
  invoke_step "$SCRIPT_DIR/start-containers.sh" "Container startup failed" "${container_args[@]}"
else
  echo "STEP 1/3 containers=skipped"
fi

if [ "$DRY_RUN" = "0" ] && [ "$SKIP_SERVICES" = "0" ]; then
  echo "STEP 2/3 infrastructure=wait"
  wait_infrastructure "$WITH_MONITORING" "1"
fi

export_service_runtime_env "$ROOT_DIR"

service_args=()
if [ "$NO_KILL_PORTS" = "1" ]; then service_args+=("--no-kill-ports"); fi
if [ "$DRY_RUN" = "1" ]; then service_args+=("--dry-run"); fi
if [ -n "$SERVICES_FILTER" ]; then service_args+=("--services=$SERVICES_FILTER"); fi

if [ "$SKIP_SERVICES" = "0" ]; then
  echo "STEP 3/3 services=start"
  invoke_step "$SCRIPT_DIR/start-services.sh" "Service startup failed" "${service_args[@]}"
else
  echo "STEP 3/3 services=skipped"
fi

dashboard_urls=(
  "http://127.0.0.1:$(docker_port_value "$ROOT_DIR" PORT_NACOS_HTTP 18848)"
  "http://127.0.0.1:$(docker_port_value "$ROOT_DIR" PORT_KIBANA_HTTP 15601)"
)
if [ "$WITH_MONITORING" = "1" ]; then
  dashboard_urls+=(
    "http://127.0.0.1:$(docker_port_value "$ROOT_DIR" PORT_PROMETHEUS_HTTP 19099)"
    "http://127.0.0.1:$(docker_port_value "$ROOT_DIR" PORT_GRAFANA_HTTP 13000)"
  )
fi
dashboard_urls+=("http://127.0.0.1:$(docker_port_value "$ROOT_DIR" PORT_SKYWALKING_UI 13001)")

if [ "$OPEN_DASHBOARDS" = "1" ] && [ "$DRY_RUN" = "0" ]; then
  for url in "${dashboard_urls[@]}"; do
    open_local_url "$url"
  done
fi

echo "PLATFORM_READY"
