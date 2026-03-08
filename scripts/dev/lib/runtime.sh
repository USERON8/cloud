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
