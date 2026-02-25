#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/lib/port-guard.sh"

WITH_MONITORING=0
KILL_PORTS=1
DRY_RUN=0

BASE_IMAGES=(
  "mysql:9.3.0"
  "redis:7.4.5-bookworm"
  "nacos/nacos-server:v3.0.2"
  "apache/rocketmq:5.3.2"
  "apacherocketmq/rocketmq-dashboard:2.1.0"
  "nginx:stable-perl"
  "minio/minio:RELEASE.2025-07-23T15-54-02Z-cpuv1"
  "elasticsearch:9.1.2"
  "kibana:9.1.2"
  "logstash:9.1.2"
)

MONITORING_IMAGES=(
  "bitnami/prometheus:3.5.0-debian-12-r3"
  "grafana/grafana:12.2.0-17084981832"
)
for arg in "$@"; do
  case "$arg" in
    --with-monitoring) WITH_MONITORING=1 ;;
    --dry-run) DRY_RUN=1 ;;
    --kill-ports) KILL_PORTS=1 ;;
    --no-kill-ports) KILL_PORTS=0 ;;
  esac
done

ENV_FILE="$ROOT_DIR/docker/.env"
if [ ! -f "$ENV_FILE" ]; then
  echo "docker/.env not found" >&2
  exit 1
fi

mapfile -t PORTS < <(awk -F= '/^PORT_[A-Z0-9_]+=/{print $2}' "$ENV_FILE")

if [ "$KILL_PORTS" = "1" ]; then
  docker_owner=0
  for p in "${PORTS[@]}"; do
    if is_docker_owner "$p"; then
      docker_owner=1
      break
    fi
  done

  if [ "$docker_owner" = "1" ] && [ "$DRY_RUN" = "0" ]; then
    (
      cd "$ROOT_DIR/docker"
      docker compose -f docker-compose.yml down >/dev/null 2>&1 || true
      docker compose -f monitoring-compose.yml down >/dev/null 2>&1 || true
    )
  fi

  for p in "${PORTS[@]}"; do
    kill_port_owner "$p" "$DRY_RUN"
  done
fi

IMAGES_TO_CHECK=("${BASE_IMAGES[@]}")
if [ "$WITH_MONITORING" = "1" ]; then
  IMAGES_TO_CHECK+=("${MONITORING_IMAGES[@]}")
fi

mapfile -t UNIQUE_IMAGES < <(printf "%s\n" "${IMAGES_TO_CHECK[@]}" | sort -u)
MISSING_IMAGES=()
for image in "${UNIQUE_IMAGES[@]}"; do
  if docker image inspect "$image" >/dev/null 2>&1; then
    echo "IMAGE_CHECK image=${image} exists=true action=use-local"
  else
    echo "IMAGE_CHECK image=${image} exists=false action=abort"
    MISSING_IMAGES+=("$image")
  fi
done

if [ "${#MISSING_IMAGES[@]}" -gt 0 ]; then
  echo "Local image check failed. Missing images: ${MISSING_IMAGES[*]}" >&2
  exit 1
fi

if [ "$DRY_RUN" = "1" ]; then
  echo "DRY_RUN_DONE script=start-containers"
  exit 0
fi

cd "$ROOT_DIR/docker"
docker compose -f docker-compose.yml up -d --pull never
if [ "$WITH_MONITORING" = "1" ]; then
  docker compose -f monitoring-compose.yml up -d --pull never prometheus grafana
fi

echo "CONTAINERS_START withMonitoring=$WITH_MONITORING status=started"
