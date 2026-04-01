#!/usr/bin/env bash
set -euo pipefail

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
  local nacos_port nacos_grpc_port nacos_grpc_offset rocketmq_namesrv_port minio_port
  local nacos_server_addr nacos_username nacos_password nacos_namespace nacos_group
  nacos_port="$(docker_port_value "$root" PORT_NACOS_HTTP 18848)"
  nacos_grpc_port="$(docker_port_value "$root" PORT_NACOS_GRPC $((nacos_port + 1000)))"
  nacos_grpc_offset="$((nacos_grpc_port - nacos_port))"
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

  export ROCKETMQ_NAMESRV_HOST="127.0.0.1"
  export ROCKETMQ_NAMESRV_PORT="$rocketmq_namesrv_port"
  export ROCKETMQ_NAME_SERVER="127.0.0.1:${rocketmq_namesrv_port}"
  export DUBBO_REGISTRY_ADDRESS="nacos://${nacos_server_addr}"
  export DUBBO_REGISTRY_USERNAME="$nacos_username"
  export DUBBO_REGISTRY_PASSWORD="$nacos_password"
  export DUBBO_REGISTRY_PARAMETERS_NAMESPACE="$nacos_namespace"
  export DUBBO_REGISTRY_PARAMETERS_GROUP="DUBBO_GROUP"
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
  export CLIENT_SERVICE_SECRET="${CLIENT_SERVICE_SECRET:-cloud-client-service-secret-dev}"
  export APP_OAUTH2_SERVICE_CLIENT_SECRET="${APP_OAUTH2_SERVICE_CLIENT_SECRET:-$CLIENT_SERVICE_SECRET}"
  export APP_OAUTH2_INTERNAL_CLIENT_SECRET="${APP_OAUTH2_INTERNAL_CLIENT_SECRET:-cloud-internal-client-secret-dev}"
  export APP_JWT_ALLOW_GENERATED_KEYPAIR="${APP_JWT_ALLOW_GENERATED_KEYPAIR:-true}"
  export GITHUB_CLIENT_ID="${GITHUB_CLIENT_ID:-cloud-github-client-dev}"
  export GITHUB_CLIENT_SECRET="${GITHUB_CLIENT_SECRET:-cloud-github-secret-dev}"

  echo "SERVICE_ENV nacos=${NACOS_SERVER_ADDR} rocketmq=${ROCKETMQ_NAME_SERVER} gatewaySignature=configured authSecrets=configured"
}
