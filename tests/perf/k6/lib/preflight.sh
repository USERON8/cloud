#!/usr/bin/env bash
set -euo pipefail

k6_preflight() {
  local mode="${1:-all}"
  local gateway_health_url="${2:-http://127.0.0.1:8080/actuator/health}"

  if [[ "${K6_SKIP_PREFLIGHT:-0}" == "1" ]]; then
    echo "[k6-preflight] skipped by K6_SKIP_PREFLIGHT=1"
    return 0
  fi

  local required_containers=()
  local required_ports=()
  if [[ "$mode" == "search" ]]; then
    required_containers=(cloud-nginx-gateway cloud-redis cloud-es-search cloud-prometheus)
    required_ports=(8080 8084 8087)
  else
    required_containers=(cloud-mysql cloud-redis cloud-nacos cloud-rmq-namesrv cloud-rmq-broker cloud-nginx-gateway cloud-es-search cloud-prometheus)
    required_ports=(8080 8081 8082 8083 8084 8085 8086 8087)
  fi

  local running
  running="$(docker ps --format '{{.Names}}' || true)"

  local missing=()
  local c
  for c in "${required_containers[@]}"; do
    if ! grep -qx "$c" <<<"$running"; then
      missing+=("$c")
    fi
  done
  if [[ ${#missing[@]} -gt 0 ]]; then
    echo "[k6-preflight] required containers are not running: ${missing[*]}" >&2
    return 1
  fi

  local closed=()
  local p
  for p in "${required_ports[@]}"; do
    if ! (exec 3<>"/dev/tcp/127.0.0.1/$p") 2>/dev/null; then
      closed+=("$p")
    else
      exec 3>&-
      exec 3<&-
    fi
  done
  if [[ ${#closed[@]} -gt 0 ]]; then
    echo "[k6-preflight] required service ports are not reachable: ${closed[*]}" >&2
    return 1
  fi

  if ! curl -fsS "$gateway_health_url" | grep -q '"status":"UP"'; then
    echo "[k6-preflight] gateway health check failed: $gateway_health_url" >&2
    return 1
  fi

  echo "[k6-preflight] passed mode=$mode"
}
