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
  local nacos_port rocketmq_namesrv_port seata_port
  nacos_port="$(docker_port_value "$root" PORT_NACOS_HTTP 18848)"
  rocketmq_namesrv_port="$(docker_port_value "$root" PORT_RMQ_NAMESRV 19876)"
  seata_port="$(docker_port_value "$root" PORT_SEATA_SERVER 18091)"

  export NACOS_HOST="127.0.0.1"
  export NACOS_PORT="$nacos_port"
  export NACOS_SERVER_ADDR="127.0.0.1:${nacos_port}"

  export ROCKETMQ_NAMESRV_HOST="127.0.0.1"
  export ROCKETMQ_NAMESRV_PORT="$rocketmq_namesrv_port"
  export ROCKETMQ_NAME_SERVER="127.0.0.1:${rocketmq_namesrv_port}"

  export SEATA_SERVER_ADDR="127.0.0.1:${seata_port}"
  export SEATA_REGISTRY_TYPE="file"
  export GATEWAY_SIGNATURE_SECRET="${GATEWAY_SIGNATURE_SECRET:-cloud-gateway-signature-dev}"
  export CLIENT_SERVICE_SECRET="${CLIENT_SERVICE_SECRET:-cloud-client-service-secret-dev}"
  export APP_OAUTH2_SERVICE_CLIENT_SECRET="${APP_OAUTH2_SERVICE_CLIENT_SECRET:-$CLIENT_SERVICE_SECRET}"
  export APP_OAUTH2_INTERNAL_CLIENT_SECRET="${APP_OAUTH2_INTERNAL_CLIENT_SECRET:-$CLIENT_SERVICE_SECRET}"
  export APP_JWT_ALLOW_GENERATED_KEYPAIR="${APP_JWT_ALLOW_GENERATED_KEYPAIR:-true}"
  export GITHUB_CLIENT_ID="${GITHUB_CLIENT_ID:-cloud-github-client-dev}"
  export GITHUB_CLIENT_SECRET="${GITHUB_CLIENT_SECRET:-cloud-github-secret-dev}"

  echo "SERVICE_ENV nacos=${NACOS_SERVER_ADDR} rocketmq=${ROCKETMQ_NAME_SERVER} seata=${SEATA_SERVER_ADDR} gatewaySignature=configured authSecrets=configured"
}
