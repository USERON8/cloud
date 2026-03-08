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
ENABLE_SKYWALKING=0
SKYWALKING_AGENT_PATH="${SKYWALKING_AGENT_PATH:-}"
SKYWALKING_BACKEND="${SKYWALKING_COLLECTOR_BACKEND_SERVICE:-}"
DRY_RUN=0

for arg in "$@"; do
  case "$arg" in
    --with-monitoring) WITH_MONITORING=1 ;;
    --no-kill-ports) NO_KILL_PORTS=1 ;;
    --skip-containers) SKIP_CONTAINERS=1 ;;
    --skip-services) SKIP_SERVICES=1 ;;
    --services=*) SERVICES_FILTER="${arg#*=}" ;;
    --open-dashboards) OPEN_DASHBOARDS=1 ;;
    --enable-skywalking) ENABLE_SKYWALKING=1 ;;
    --dry-run) DRY_RUN=1 ;;
    --skywalking-agent-path=*) SKYWALKING_AGENT_PATH="${arg#*=}" ;;
    --skywalking-backend=*) SKYWALKING_BACKEND="${arg#*=}" ;;
  esac
done

invoke_step() {
  local script_path="$1"
  local error_message="$2"
  shift 2
  "$script_path" "$@"
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
    "rocketmq-namesrv|$(docker_port_value "$ROOT_DIR" PORT_RMQ_NAMESRV 19876)"
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

  local target name port
  for target in "${targets[@]}"; do
    IFS='|' read -r name port <<< "$target"
    echo "WAIT_PORT name=$name port=$port status=waiting"
    if ! wait_tcp_port "127.0.0.1" "$port" 120; then
      echo "Infrastructure port not ready: $name:$port" >&2
      exit 1
    fi
    echo "WAIT_PORT name=$name port=$port status=ready"
  done
}

configure_skywalking() {
  local resolved_agent_path="$SKYWALKING_AGENT_PATH"
  if [ -n "$resolved_agent_path" ] && [ ! -f "$resolved_agent_path" ]; then
    if [ "$ENABLE_SKYWALKING" = "1" ]; then
      echo "SkyWalking agent not found: $resolved_agent_path" >&2
      exit 1
    fi
    resolved_agent_path=""
  fi

  if [ -z "$resolved_agent_path" ]; then
    if [ "$ENABLE_SKYWALKING" = "1" ]; then
      echo "SkyWalking requested but no agent path was provided." >&2
      exit 1
    fi
    return 1
  fi

  if [ -z "$SKYWALKING_BACKEND" ]; then
    SKYWALKING_BACKEND="127.0.0.1:$(docker_port_value "$ROOT_DIR" PORT_SKYWALKING_OAP_GRPC 11800)"
  fi

  export SKYWALKING_AGENT_PATH="$resolved_agent_path"
  export SKYWALKING_COLLECTOR_BACKEND_SERVICE="$SKYWALKING_BACKEND"
  echo "SKYWALKING enabled=true agent=$resolved_agent_path backend=$SKYWALKING_BACKEND"
  return 0
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

SKYWALKING_ACTIVE=0
if configure_skywalking; then
  SKYWALKING_ACTIVE=1
fi

if [ "$DRY_RUN" = "0" ] && [ "$SKIP_SERVICES" = "0" ]; then
  echo "STEP 2/3 infrastructure=wait"
  wait_infrastructure "$WITH_MONITORING" "$SKYWALKING_ACTIVE"
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
if [ "$SKYWALKING_ACTIVE" = "1" ]; then
  dashboard_urls+=("http://127.0.0.1:$(docker_port_value "$ROOT_DIR" PORT_SKYWALKING_UI 13001)")
fi

if [ "$OPEN_DASHBOARDS" = "1" ] && [ "$DRY_RUN" = "0" ]; then
  for url in "${dashboard_urls[@]}"; do
    open_local_url "$url"
  done
fi

echo "PLATFORM_READY"
