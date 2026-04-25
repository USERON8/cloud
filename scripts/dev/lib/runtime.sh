#!/usr/bin/env bash
set -euo pipefail

trim_env_value() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

read_env_file() {
  local path="$1"
  local keys_name="$2"
  local values_name="$3"
  declare -n keys_ref="$keys_name"
  declare -n values_ref="$values_name"

  keys_ref=()
  values_ref=()
  if [ ! -f "$path" ]; then
    return 0
  fi

  local line trimmed key value
  while IFS= read -r line || [ -n "$line" ]; do
    trimmed="$(trim_env_value "$line")"
    if [ -z "$trimmed" ] || [[ "$trimmed" == \#* ]]; then
      continue
    fi
    key="$(trim_env_value "${trimmed%%=*}")"
    value="${trimmed#*=}"
    if [ -z "$key" ]; then
      continue
    fi
    if [ -z "${values_ref[$key]+_}" ]; then
      keys_ref+=("$key")
    fi
    values_ref["$key"]="$value"
  done < "$path"
}

write_env_file() {
  local path="$1"
  local keys_name="$2"
  local values_name="$3"
  declare -n keys_ref="$keys_name"
  declare -n values_ref="$values_name"

  local line_buffer=()
  local key
  for key in "${keys_ref[@]}"; do
    line_buffer+=("${key}=${values_ref[$key]}")
  done
  printf '%s\n' "${line_buffer[@]}" > "$path"
}

set_env_value() {
  local keys_name="$1"
  local values_name="$2"
  local name="$3"
  local value="${4:-}"
  declare -n keys_ref="$keys_name"
  declare -n values_ref="$values_name"

  if [ -z "${values_ref[$name]+_}" ]; then
    keys_ref+=("$name")
  fi
  values_ref["$name"]="$value"
}

get_preferred_local_ipv4() {
  local ip_addr=""
  if command -v ip >/dev/null 2>&1; then
    ip_addr="$(ip route get 1 2>/dev/null | awk '{for (i = 1; i <= NF; i++) if ($i == "src") {print $(i + 1); exit}}')"
  fi
  if [ -z "$ip_addr" ] && command -v hostname >/dev/null 2>&1; then
    ip_addr="$(hostname -I 2>/dev/null | awk '{print $1}')"
  fi
  case "$ip_addr" in
    127.*|169.254.*) ip_addr="" ;;
  esac
  printf '%s\n' "$ip_addr"
}

sync_environment_files() {
  local root="$1"
  local import_process_environment="${2:-0}"
  local emit_log="${3:-1}"
  local script_path="$root/scripts/dev/lib/runtime-sync.mjs"
  local preferred_local_ipv4 json

  if ! command -v node >/dev/null 2>&1; then
    echo "node is required to synchronize development environment files" >&2
    return 1
  fi

  preferred_local_ipv4="$(get_preferred_local_ipv4)"
  if [ -n "$preferred_local_ipv4" ]; then
    json="$(node "$script_path" "$root" "--preferred-local-ipv4=$preferred_local_ipv4")"
  else
    json="$(node "$script_path" "$root")"
  fi

  if [ "$import_process_environment" = "1" ]; then
    while IFS= read -r line; do
      [ -n "$line" ] || continue
      local key="${line%%=*}"
      local value="${line#*=}"
      export "$key=$value"
    done < <(
      printf '%s' "$json" | node -e 'const fs=require("node:fs"); const data=JSON.parse(fs.readFileSync(0,"utf8")); for (const source of [data.dockerEnv, data.frontendEnv]) { for (const [key, value] of Object.entries(source)) { process.stdout.write(`${key}=${value ?? ""}\n`); } }'
    )
  fi

  if [ "$emit_log" = "1" ]; then
    printf '%s\n' "$(
      printf '%s' "$json" | node -e 'const fs=require("node:fs"); const data=JSON.parse(fs.readFileSync(0,"utf8")); process.stdout.write(`ENV_SYNC root=${data.rootEnvPath} docker=${data.dockerEnvPath} frontend=${data.frontendDevEnvPath}`);'
    )"
  fi
}

docker_env_value() {
  local root="$1"
  local name="$2"
  local default_value="${3:-}"
  local env_file="$root/docker/.env"
  if [ ! -f "$env_file" ]; then
    printf '%s\n' "$default_value"
    return 0
  fi

  local value
  value="$(awk -F= -v key="$name" '$1 == key {print substr($0, index($0, "=") + 1); exit}' "$env_file")"
  if [ -z "$value" ]; then
    printf '%s\n' "$default_value"
  else
    printf '%s\n' "$value"
  fi
}

docker_port_value() {
  local root="$1"
  local name="$2"
  local default_value="$3"
  local value
  value="$(docker_env_value "$root" "$name" "$default_value")"
  if [[ "$value" =~ ^[0-9]+$ ]]; then
    printf '%s\n' "$value"
  else
    printf '%s\n' "$default_value"
  fi
}

wait_tcp_port() {
  local host="$1"
  local port="$2"
  local timeout="${3:-90}"
  local start now
  start="$(date +%s)"
  while true; do
    if (echo >"/dev/tcp/$host/$port") >/dev/null 2>&1; then
      return 0
    fi
    now="$(date +%s)"
    if [ $((now - start)) -ge "$timeout" ]; then
      return 1
    fi
    sleep 1
  done
}

open_local_url() {
  local url="$1"
  if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$url" >/dev/null 2>&1 || true
    return 0
  fi
  if command -v open >/dev/null 2>&1; then
    open "$url" >/dev/null 2>&1 || true
    return 0
  fi
  if command -v cmd.exe >/dev/null 2>&1; then
    cmd.exe /c start "" "$url" >/dev/null 2>&1 || true
  fi
}

export_service_runtime_env() {
  local root="$1"
  local nacos_port nacos_grpc_port nacos_grpc_offset redis_port rocketmq_namesrv_port minio_port
  local nacos_server_addr nacos_username nacos_password nacos_namespace nacos_group
  sync_environment_files "$root" 1 0
  nacos_port="$(docker_port_value "$root" PORT_NACOS_HTTP 18848)"
  nacos_grpc_port="$(docker_port_value "$root" PORT_NACOS_GRPC $((nacos_port + 1000)))"
  nacos_grpc_offset="$((nacos_grpc_port - nacos_port))"
  redis_port="$(docker_port_value "$root" PORT_REDIS 16379)"
  rocketmq_namesrv_port="$(docker_port_value "$root" PORT_RMQ_NAMESRV 20011)"
  minio_port="$(docker_port_value "$root" PORT_MINIO_API 19000)"
  nacos_server_addr="127.0.0.1:${nacos_port}"
  nacos_username="${NACOS_USERNAME:-nacos}"
  nacos_password="${NACOS_PASSWORD:-nacos}"
  nacos_namespace="${NACOS_NAMESPACE:-public}"
  nacos_group="${NACOS_GROUP:-DEFAULT_GROUP}"

  export NACOS_HOST="127.0.0.1"
  export NACOS_PORT="$nacos_port"
  export NACOS_SERVER_ADDR="$nacos_server_addr"
  export NACOS_CONFIG_SERVER_ADDR="$nacos_server_addr"
  export NACOS_DISCOVERY_SERVER_ADDR="$nacos_server_addr"
  export NACOS_USERNAME="$nacos_username"
  export NACOS_PASSWORD="$nacos_password"
  export NACOS_NAMESPACE="$nacos_namespace"
  export NACOS_GROUP="$nacos_group"
  export NACOS_CONFIG_NAMESPACE="$nacos_namespace"
  export NACOS_CONFIG_GROUP="$nacos_group"
  export NACOS_DISCOVERY_NAMESPACE="$nacos_namespace"
  export NACOS_DISCOVERY_GROUP="$nacos_group"
  export SPRING_CLOUD_NACOS_SERVER_ADDR="$nacos_server_addr"
  export SPRING_CLOUD_NACOS_USERNAME="$nacos_username"
  export SPRING_CLOUD_NACOS_PASSWORD="$nacos_password"
  export SPRING_CLOUD_NACOS_NAMESPACE="$nacos_namespace"
  export SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR="$nacos_server_addr"
  export SPRING_CLOUD_NACOS_CONFIG_USERNAME="$nacos_username"
  export SPRING_CLOUD_NACOS_CONFIG_PASSWORD="$nacos_password"
  export SPRING_CLOUD_NACOS_CONFIG_NAMESPACE="$nacos_namespace"
  export SPRING_CLOUD_NACOS_CONFIG_GROUP="$nacos_group"
  export SPRING_CLOUD_NACOS_CONFIG_FILE_EXTENSION="yaml"
  export SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR="$nacos_server_addr"
  export SPRING_CLOUD_NACOS_DISCOVERY_USERNAME="$nacos_username"
  export SPRING_CLOUD_NACOS_DISCOVERY_PASSWORD="$nacos_password"
  export SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE="$nacos_namespace"
  export SPRING_CLOUD_NACOS_DISCOVERY_GROUP="$nacos_group"
  if [ "$nacos_grpc_offset" -gt 0 ]; then
    export NACOS_GRPC_PORT_OFFSET="$nacos_grpc_offset"
  fi

  export REDIS_HOST="127.0.0.1"
  export REDIS_PORT="$redis_port"
  export SPRING_DATA_REDIS_HOST="$REDIS_HOST"
  export SPRING_DATA_REDIS_PORT="$REDIS_PORT"
  export SPRING_REDIS_HOST="$REDIS_HOST"
  export SPRING_REDIS_PORT="$REDIS_PORT"

  export ROCKETMQ_NAMESRV_HOST="127.0.0.1"
  export ROCKETMQ_NAMESRV_PORT="$rocketmq_namesrv_port"
  export ROCKETMQ_NAME_SERVER="127.0.0.1:${rocketmq_namesrv_port}"
  export DUBBO_REGISTRY_ADDRESS="nacos://${nacos_server_addr}"
  export DUBBO_REGISTRY_USERNAME="$nacos_username"
  export DUBBO_REGISTRY_PASSWORD="$nacos_password"
  export DUBBO_REGISTRY_PARAMETERS_NAMESPACE="$nacos_namespace"
  export DUBBO_REGISTRY_PARAMETERS_GROUP="DUBBO_GROUP"
  export GATEWAY_ROUTE_AUTH_URI="http://127.0.0.1:8081"
  export GATEWAY_ROUTE_USER_URI="http://127.0.0.1:8082"
  export GATEWAY_ROUTE_ORDER_URI="http://127.0.0.1:8083"
  export GATEWAY_ROUTE_PRODUCT_URI="http://127.0.0.1:8084"
  export GATEWAY_ROUTE_STOCK_URI="http://127.0.0.1:8085"
  export GATEWAY_ROUTE_PAYMENT_URI="http://127.0.0.1:8086"
  export GATEWAY_ROUTE_SEARCH_URI="http://127.0.0.1:8087"
  export GATEWAY_ROUTE_GOVERNANCE_URI="http://127.0.0.1:8088"
  if [ -z "${DUBBO_APPLICATION_QOS_ENABLE:-}" ]; then
    export DUBBO_APPLICATION_QOS_ENABLE="false"
  fi

  if [ -z "${XXL_JOB_ENABLED:-}" ]; then
    export XXL_JOB_ENABLED="false"
  fi
  if [ -z "${SKYWALKING_ENABLED:-}" ]; then
    export SKYWALKING_ENABLED="false"
  fi
  export MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://127.0.0.1:${minio_port}}"
  export MINIO_PUBLIC_ENDPOINT="${MINIO_PUBLIC_ENDPOINT:-$MINIO_ENDPOINT}"
  export MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-minioadmin}"
  export MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-minioadmin}"
  export GATEWAY_SIGNATURE_SECRET="${GATEWAY_SIGNATURE_SECRET:-cloud-gateway-signature-dev}"
  export GATEWAY_INTERNAL_IDENTITY_SECRET="${GATEWAY_INTERNAL_IDENTITY_SECRET:-$GATEWAY_SIGNATURE_SECRET}"
  export CLIENT_SERVICE_SECRET="${CLIENT_SERVICE_SECRET:-cloud-client-service-secret-dev}"
  export APP_OAUTH2_SERVICE_CLIENT_SECRET="${APP_OAUTH2_SERVICE_CLIENT_SECRET:-$CLIENT_SERVICE_SECRET}"
  export APP_OAUTH2_INTERNAL_CLIENT_SECRET="${APP_OAUTH2_INTERNAL_CLIENT_SECRET:-cloud-internal-client-secret-dev}"
  export APP_JWT_ALLOW_GENERATED_KEYPAIR="${APP_JWT_ALLOW_GENERATED_KEYPAIR:-true}"
  export GITHUB_CLIENT_ID="${GITHUB_CLIENT_ID:-cloud-github-client-dev}"
  export GITHUB_CLIENT_SECRET="${GITHUB_CLIENT_SECRET:-cloud-github-secret-dev}"

  echo "SERVICE_ENV nacos=${NACOS_SERVER_ADDR} rocketmq=${ROCKETMQ_NAME_SERVER} gatewaySignature=configured authSecrets=configured"
}
