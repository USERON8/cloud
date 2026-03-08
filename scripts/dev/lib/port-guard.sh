#!/usr/bin/env bash
set -euo pipefail

has_cmd() {
  command -v "$1" >/dev/null 2>&1
}

get_port_owner_lines() {
  local port="$1"
  if has_cmd lsof; then
    lsof -nP -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | awk 'NR>1 {print $2" "$1}' | sort -u
    return 0
  fi
  if has_cmd ss; then
    ss -ltnp "( sport = :$port )" 2>/dev/null | awk -F 'pid=|,' '/pid=/ {print $2}' | while read -r pid; do
      [ -z "${pid:-}" ] && continue
      local name
      name="$(ps -p "$pid" -o comm= 2>/dev/null || echo unknown)"
      echo "$pid $name"
    done | sort -u
    return 0
  fi
  if has_cmd netstat; then
    netstat -lntp 2>/dev/null | awk -v p=":$port" '$4 ~ p"$" {print $7}' | awk -F'/' '{print $1" "$2}' | sort -u
    return 0
  fi
  return 1
}

wait_port_free() {
  local port="$1"
  local timeout="${2:-8}"
  local start now
  start="$(date +%s)"
  while true; do
    if ! get_port_owner_lines "$port" | grep -q .; then
      return 0
    fi
    now="$(date +%s)"
    if [ $((now - start)) -ge "$timeout" ]; then
      return 1
    fi
    sleep 0.3
  done
}

kill_port_owner() {
  local port="$1"
  local dry_run="${2:-0}"
  local lines
  lines="$(get_port_owner_lines "$port" || true)"
  if [ -z "$lines" ]; then
    echo "PORT_GUARD port=$port pid=- process=- action=skip reason=free"
    return 0
  fi

  while read -r pid name; do
    [ -z "${pid:-}" ] && continue
    if [ "$dry_run" = "1" ]; then
      echo "PORT_GUARD port=$port pid=$pid process=$name action=dry-run-kill"
      continue
    fi
    if kill -9 "$pid" >/dev/null 2>&1; then
      if wait_port_free "$port" 8; then
        echo "PORT_GUARD port=$port pid=$pid process=$name action=killed state=freed"
      else
        echo "PORT_GUARD port=$port pid=$pid process=$name action=killed state=occupied"
      fi
    else
      echo "PORT_GUARD port=$port pid=$pid process=$name action=kill-failed"
    fi
  done <<< "$lines"
}

is_docker_owner() {
  local port="$1"
  local lines
  lines="$(get_port_owner_lines "$port" || true)"
  if [ -z "$lines" ]; then
    return 1
  fi
  if echo "$lines" | awk '{print tolower($2)}' | grep -Eq 'docker|com\.docker'; then
    return 0
  fi
  return 1
}
