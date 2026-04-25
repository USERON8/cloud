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

first_non_empty_value() {
  local candidate trimmed
  for candidate in "$@"; do
    trimmed="$(trim_env_value "$candidate")"
    if [ -n "$trimmed" ]; then
      printf '%s\n' "$trimmed"
      return 0
    fi
  done
  printf '\n'
}

resolve_public_base_url() {
  local url
  url="$(trim_env_value "${1:-}")"
  if [ -z "$url" ]; then
    printf '\n'
    return 0
  fi

  url="${url%/}"
  if [[ "$url" =~ ^https?://[^/]+ ]]; then
    printf '%s\n' "${BASH_REMATCH[0]}"
  else
    printf '%s\n' "$url"
  fi
}

parse_cpolar_domain_map() {
  local raw_value="$1"
  local values_name="$2"
  declare -n values_ref="$values_name"

  values_ref=()
  if [ -z "$(trim_env_value "$raw_value")" ]; then
    return 0
  fi

  local entry trimmed_entry key value
  IFS=',' read -r -a entries <<< "$raw_value"
  for entry in "${entries[@]}"; do
    trimmed_entry="$(trim_env_value "$entry")"
    if [ -z "$trimmed_entry" ] || [[ "$trimmed_entry" != *=* ]]; then
      continue
    fi
    key="$(trim_env_value "${trimmed_entry%%=*}")"
    key="${key,,}"
    value="$(resolve_public_base_url "${trimmed_entry#*=}")"
    if [ -n "$key" ] && [ -n "$value" ]; then
      values_ref["$key"]="$value"
    fi
  done
}

build_cpolar_domain_map() {
  local public_base_url="$1"
  local frontend_base_url="$2"
  local parts=()

  if [ -n "$public_base_url" ]; then
    parts+=("public=${public_base_url}")
  fi
  if [ -n "$frontend_base_url" ]; then
    parts+=("frontend=${frontend_base_url}")
  fi

  local joined=""
  local part
  for part in "${parts[@]}"; do
    if [ -n "$joined" ]; then
      joined+=","
    fi
    joined+="$part"
  done
  printf '%s\n' "$joined"
}

add_unique_item() {
  local items_name="$1"
  local item="$2"
  declare -n items_ref="$items_name"

  if [ -z "$item" ]; then
    return 0
  fi

  local existing
  for existing in "${items_ref[@]:-}"; do
    if [ "$existing" = "$item" ]; then
      return 0
    fi
  done
  items_ref+=("$item")
}

append_origin_variants() {
  local items_name="$1"
  local base_url="$2"
  local resolved_base_url
  resolved_base_url="$(resolve_public_base_url "$base_url")"
  if [ -z "$resolved_base_url" ]; then
    return 0
  fi

  add_unique_item "$items_name" "$resolved_base_url"
  if [[ "$resolved_base_url" == http://* ]]; then
    add_unique_item "$items_name" "https://${resolved_base_url#http://}"
  fi
}

join_csv() {
  local values=("$@")
  local result=""
  local value
  for value in "${values[@]}"; do
    if [ -n "$result" ]; then
      result+=","
    fi
    result+="$value"
  done
  printf '%s\n' "$result"
}

import_env_map() {
  local values_name="$1"
  declare -n values_ref="$values_name"
  local key
  for key in "${!values_ref[@]}"; do
    export "$key=${values_ref[$key]}"
  done
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

  local root_env_path="$root/.env"
  local docker_env_path="$root/docker/.env"
  local frontend_dev_env_path="$root/my-shop-uniapp/.env.development"
  local frontend_prod_env_path="$root/my-shop-uniapp/.env.production"
  local nginx_http_port local_gateway_base_url preferred_local_ipv4 gateway_upstream auth_upstream
  local root_public_base_url root_domain_base_url docker_public_base_url docker_domain_base_url
  local root_frontend_base_url docker_frontend_base_url cpolar_domain_map_raw public_base_url frontend_base_url
  local cpolar_domain_map_value oauth_redirect_uris_value origin_patterns_value
  local target key

  declare -a root_env_keys=()
  declare -a docker_env_keys=()
  declare -a frontend_env_keys=()
  declare -a oauth_redirect_uris=()
  declare -a origin_patterns=()
  declare -A root_env_values=()
  declare -A docker_env_values=()
  declare -A frontend_env_values=()
  declare -A cpolar_domain_map=()

  read_env_file "$root_env_path" root_env_keys root_env_values
  read_env_file "$docker_env_path" docker_env_keys docker_env_values

  for key in "${root_env_keys[@]}"; do
    set_env_value docker_env_keys docker_env_values "$key" "${root_env_values[$key]}"
  done

  nginx_http_port="${docker_env_values[PORT_NGINX_HTTP]:-18080}"
  local_gateway_base_url="http://127.0.0.1:${nginx_http_port}"
  preferred_local_ipv4="$(get_preferred_local_ipv4)"
  if [ -n "$preferred_local_ipv4" ]; then
    gateway_upstream="${preferred_local_ipv4}:8080"
    auth_upstream="${preferred_local_ipv4}:8081"
  else
    gateway_upstream="host.docker.internal:8080"
    auth_upstream="host.docker.internal:8081"
  fi

  root_public_base_url="$(resolve_public_base_url "${root_env_values[CPOLAR_PUBLIC_BASE_URL]:-}")"
  root_domain_base_url="$(resolve_public_base_url "${root_env_values[CPOLAR_DOMAIN]:-}")"
  docker_public_base_url="$(resolve_public_base_url "${docker_env_values[CPOLAR_PUBLIC_BASE_URL]:-}")"
  docker_domain_base_url="$(resolve_public_base_url "${docker_env_values[CPOLAR_DOMAIN]:-}")"
  root_frontend_base_url="$(resolve_public_base_url "${root_env_values[CPOLAR_FRONTEND_BASE_URL]:-}")"
  docker_frontend_base_url="$(resolve_public_base_url "${docker_env_values[CPOLAR_FRONTEND_BASE_URL]:-}")"
  cpolar_domain_map_raw="$(first_non_empty_value "${root_env_values[CPOLAR_DOMAIN_MAP]:-}" "${docker_env_values[CPOLAR_DOMAIN_MAP]:-}")"
  parse_cpolar_domain_map "$cpolar_domain_map_raw" cpolar_domain_map
  public_base_url="$(first_non_empty_value "${cpolar_domain_map[public]:-}" "$root_public_base_url" "$root_domain_base_url" "$docker_public_base_url" "$docker_domain_base_url" "$local_gateway_base_url")"
  frontend_base_url="$(first_non_empty_value "${cpolar_domain_map[frontend]:-}" "$root_frontend_base_url" "$docker_frontend_base_url" "$public_base_url")"
  cpolar_domain_map_value="$(build_cpolar_domain_map "$public_base_url" "$frontend_base_url")"

  set_env_value docker_env_keys docker_env_values "NGINX_GATEWAY_UPSTREAM" "$gateway_upstream"
  set_env_value docker_env_keys docker_env_values "NGINX_AUTH_UPSTREAM" "$auth_upstream"
  set_env_value root_env_keys root_env_values "CPOLAR_DOMAIN_MAP" "$cpolar_domain_map_value"
  set_env_value root_env_keys root_env_values "CPOLAR_DOMAIN" "$public_base_url"
  set_env_value root_env_keys root_env_values "CPOLAR_PUBLIC_BASE_URL" "$public_base_url"
  set_env_value root_env_keys root_env_values "CPOLAR_FRONTEND_BASE_URL" "$frontend_base_url"
  set_env_value docker_env_keys docker_env_values "CPOLAR_DOMAIN_MAP" "$cpolar_domain_map_value"
  set_env_value docker_env_keys docker_env_values "CPOLAR_DOMAIN" "$public_base_url"
  set_env_value docker_env_keys docker_env_values "CPOLAR_PUBLIC_BASE_URL" "$public_base_url"
  set_env_value docker_env_keys docker_env_values "CPOLAR_FRONTEND_BASE_URL" "$frontend_base_url"

  add_unique_item oauth_redirect_uris "${frontend_base_url}/callback"
  add_unique_item oauth_redirect_uris "${public_base_url}/callback"
  add_unique_item oauth_redirect_uris "http://127.0.0.1:${nginx_http_port}/callback"
  add_unique_item oauth_redirect_uris "http://127.0.0.1:3000/callback"
  add_unique_item oauth_redirect_uris "http://127.0.0.1:5173/callback"
  add_unique_item oauth_redirect_uris "http://localhost:5173/callback"
  oauth_redirect_uris_value="$(join_csv "${oauth_redirect_uris[@]}")"

  add_unique_item origin_patterns "http://127.0.0.1:*"
  add_unique_item origin_patterns "https://127.0.0.1:*"
  add_unique_item origin_patterns "http://localhost:*"
  add_unique_item origin_patterns "https://localhost:*"
  append_origin_variants origin_patterns "$public_base_url"
  append_origin_variants origin_patterns "$frontend_base_url"
  origin_patterns_value="$(join_csv "${origin_patterns[@]}")"

  for target in root docker; do
    local env_keys_name env_values_name
    if [ "$target" = "root" ]; then
      env_keys_name="root_env_keys"
      env_values_name="root_env_values"
    else
      env_keys_name="docker_env_keys"
      env_values_name="docker_env_values"
    fi
    set_env_value "$env_keys_name" "$env_values_name" "ALIPAY_NOTIFY_URL" "${public_base_url}/api/v1/payment/alipay/notify"
    set_env_value "$env_keys_name" "$env_values_name" "ALIPAY_RETURN_URL" "${frontend_base_url}/#/pages/app/payments/index"
    set_env_value "$env_keys_name" "$env_values_name" "GITHUB_REDIRECT_URI" "${public_base_url}/login/oauth2/code/github"
    set_env_value "$env_keys_name" "$env_values_name" "APP_OAUTH2_GITHUB_ERROR_URL" "${frontend_base_url}/auth/error"
    set_env_value "$env_keys_name" "$env_values_name" "APP_OAUTH2_WEB_REDIRECT_URIS" "$oauth_redirect_uris_value"
    set_env_value "$env_keys_name" "$env_values_name" "APP_SECURITY_CORS_ALLOWED_ORIGIN_PATTERNS" "$origin_patterns_value"
  done

  write_env_file "$root_env_path" root_env_keys root_env_values
  write_env_file "$docker_env_path" docker_env_keys docker_env_values

  set_env_value frontend_env_keys frontend_env_values "VITE_API_BASE_URL" "$public_base_url"
  set_env_value frontend_env_keys frontend_env_values "VITE_DEV_PROXY_TARGET" "$local_gateway_base_url"
  set_env_value frontend_env_keys frontend_env_values "VITE_CPOLAR_DOMAIN" "$frontend_base_url"
  set_env_value frontend_env_keys frontend_env_values "VITE_OAUTH_CLIENT_ID" "web-client"
  set_env_value frontend_env_keys frontend_env_values "VITE_OAUTH_REDIRECT_URI" "${frontend_base_url}/callback"
  set_env_value frontend_env_keys frontend_env_values "VITE_SEARCH_FALLBACK_TIMEOUT" "5000"

  write_env_file "$frontend_dev_env_path" frontend_env_keys frontend_env_values
  write_env_file "$frontend_prod_env_path" frontend_env_keys frontend_env_values

  if [ "$import_process_environment" = "1" ]; then
    import_env_map docker_env_values
    import_env_map frontend_env_values
  fi

  if [ "$emit_log" = "1" ]; then
    echo "ENV_SYNC root=${root_env_path} docker=${docker_env_path} frontend=${frontend_dev_env_path}"
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
