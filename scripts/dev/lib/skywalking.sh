#!/usr/bin/env bash
set -euo pipefail

skywalking_is_truthy() {
  case "${1:-}" in
    1|true|TRUE|True|yes|YES|Yes|on|ON|On) return 0 ;;
    0|false|FALSE|False|no|NO|No|off|OFF|Off) return 1 ;;
    "") return 1 ;;
    *) return 1 ;;
  esac
}

skywalking_canonical_file() {
  local file_path="$1"
  if [ ! -f "$file_path" ]; then
    return 1
  fi
  (
    cd "$(dirname "$file_path")"
    printf '%s/%s\n' "$(pwd)" "$(basename "$file_path")"
  )
}

skywalking_agent_version() {
  local root="$1"
  if [ -n "${SKYWALKING_AGENT_VERSION:-}" ]; then
    printf '%s\n' "$SKYWALKING_AGENT_VERSION"
    return 0
  fi

  local pom_file="$root/pom.xml"
  local pom_version=""
  if [ -f "$pom_file" ]; then
    pom_version="$(awk -F'[<>]' '/<skywalking.version>/ {print $3; exit}' "$pom_file")"
  fi
  if [ -n "$pom_version" ]; then
    printf '%s\n' "$pom_version"
    return 0
  fi

  printf '9.6.0\n'
}

skywalking_cache_root() {
  local root="$1"
  if [ -n "${SKYWALKING_CACHE_DIR:-}" ]; then
    printf '%s\n' "$SKYWALKING_CACHE_DIR"
    return 0
  fi
  printf '%s/.tmp/skywalking\n' "$root"
}

skywalking_find_cached_agent() {
  local root="$1"
  local version="$2"
  local install_root
  install_root="$(skywalking_cache_root "$root")/agent/${version}"
  if [ ! -d "$install_root" ]; then
    return 1
  fi
  find "$install_root" -path '*/skywalking-agent.jar' -type f | head -n 1
}

skywalking_archive_is_valid() {
  local archive_path="$1"
  [ -f "$archive_path" ] && tar -tzf "$archive_path" >/dev/null 2>&1
}

skywalking_enable_optional_plugins() {
  local agent_jar_path="$1"
  local agent_home optional_plugins_dir plugins_dir plugin_glob plugin_path
  local optional_patterns="${SKYWALKING_OPTIONAL_PLUGINS:-}"

  agent_home="$(dirname "$agent_jar_path")"
  optional_plugins_dir="$agent_home/optional-plugins"
  plugins_dir="$agent_home/plugins"
  if [ ! -d "$optional_plugins_dir" ] || [ ! -d "$plugins_dir" ]; then
    return 0
  fi

  for plugin_glob in $optional_patterns; do
    while IFS= read -r -d '' plugin_path; do
      cp -f "$plugin_path" "$plugins_dir/"
    done < <(find "$optional_plugins_dir" -maxdepth 1 -type f -name "$plugin_glob" -print0)
  done
}

skywalking_download_agent() {
  local root="$1"
  local version="$2"
  local cache_root archive_dir archive_path archive_tmp_path extract_root staging_dir download_url resolved_agent download_timeout download_urls
  cache_root="$(skywalking_cache_root "$root")"
  archive_dir="$cache_root/downloads"
  archive_path="$archive_dir/apache-skywalking-java-agent-${version}.tgz"
  archive_tmp_path="${archive_path}.partial.$$"
  extract_root="$cache_root/agent/${version}"
  download_timeout="${SKYWALKING_AGENT_DOWNLOAD_TIMEOUT_SECONDS:-180}"
  if [ -n "${SKYWALKING_AGENT_DOWNLOAD_URL:-}" ]; then
    download_urls="${SKYWALKING_AGENT_DOWNLOAD_URL}"
  else
    download_urls="https://downloads.apache.org/skywalking/java-agent/${version}/apache-skywalking-java-agent-${version}.tgz https://archive.apache.org/dist/skywalking/java-agent/${version}/apache-skywalking-java-agent-${version}.tgz"
  fi

  mkdir -p "$archive_dir"
  if ! skywalking_archive_is_valid "$archive_path"; then
    rm -f "$archive_path" "$archive_tmp_path"
    for download_url in $download_urls; do
      if command -v curl >/dev/null 2>&1; then
        if ! curl -fsSL --connect-timeout 15 --max-time "$download_timeout" "$download_url" -o "$archive_tmp_path"; then
          rm -f "$archive_tmp_path"
          continue
        fi
      elif command -v wget >/dev/null 2>&1; then
        if ! wget -q --timeout="$download_timeout" -O "$archive_tmp_path" "$download_url"; then
          rm -f "$archive_tmp_path"
          continue
        fi
      else
        echo "SkyWalking auto download requires curl or wget." >&2
        return 1
      fi
      mv "$archive_tmp_path" "$archive_path"
      break
    done
  fi

  if ! skywalking_archive_is_valid "$archive_path"; then
    rm -f "$archive_path"
    echo "SkyWalking agent archive validation failed." >&2
    return 1
  fi

  staging_dir="$cache_root/extract-${version}-$$"
  rm -rf "$staging_dir" "$extract_root"
  mkdir -p "$staging_dir" "$extract_root"
  tar -xzf "$archive_path" -C "$staging_dir"
  find "$staging_dir" -mindepth 1 -maxdepth 1 -exec mv {} "$extract_root"/ \;
  rm -rf "$staging_dir"

  resolved_agent="$(skywalking_find_cached_agent "$root" "$version" || true)"
  if [ -z "$resolved_agent" ]; then
    echo "SkyWalking agent archive extracted but skywalking-agent.jar was not found." >&2
    return 1
  fi
  skywalking_enable_optional_plugins "$resolved_agent"
  printf '%s\n' "$resolved_agent"
}

configure_skywalking_runtime() {
  local root="$1"
  local requested="${2:-0}"
  local preferred_agent_path="${3:-}"
  local preferred_backend="${4:-}"
  local allow_download="${5:-1}"
  local explicit_agent_path auto_enable version resolved_agent_path resolved_backend

  version="$(skywalking_agent_version "$root")"
  explicit_agent_path="$preferred_agent_path"
  if [ -z "$explicit_agent_path" ] && [ -n "${SKYWALKING_AGENT_PATH:-}" ]; then
    explicit_agent_path="${SKYWALKING_AGENT_PATH}"
  fi

  auto_enable=1
  if [ -n "${SKYWALKING_AUTO_ENABLE:-}" ] && ! skywalking_is_truthy "${SKYWALKING_AUTO_ENABLE}"; then
    auto_enable=0
  fi

  resolved_agent_path=""
  if [ -n "$explicit_agent_path" ]; then
    if ! resolved_agent_path="$(skywalking_canonical_file "$explicit_agent_path")"; then
      if [ "$requested" = "1" ]; then
        echo "SkyWalking agent not found: $explicit_agent_path" >&2
        return 2
      fi
      echo "SKYWALKING enabled=false reason=agent-not-found path=$explicit_agent_path"
      return 1
    fi
  elif [ "$requested" = "1" ] || [ "$auto_enable" = "1" ]; then
    resolved_agent_path="$(skywalking_find_cached_agent "$root" "$version" || true)"
    if [ -z "$resolved_agent_path" ] && [ "$allow_download" = "1" ]; then
      if ! resolved_agent_path="$(skywalking_download_agent "$root" "$version")"; then
        if [ "$requested" = "1" ]; then
          echo "SkyWalking agent download failed for version $version." >&2
          return 2
        fi
        echo "SKYWALKING enabled=false reason=download-failed version=$version"
        return 1
      fi
    fi
  else
    echo "SKYWALKING enabled=false reason=disabled"
    return 1
  fi

  if [ -z "$resolved_agent_path" ]; then
    if [ "$requested" = "1" ]; then
      echo "SkyWalking requested but the agent is unavailable." >&2
      return 2
    fi
    echo "SKYWALKING enabled=false reason=agent-unavailable version=$version"
    return 1
  fi

  if [ -n "$preferred_backend" ]; then
    resolved_backend="$preferred_backend"
  elif [ -n "${SKYWALKING_COLLECTOR_BACKEND_SERVICE:-}" ]; then
    resolved_backend="${SKYWALKING_COLLECTOR_BACKEND_SERVICE}"
  else
    resolved_backend="127.0.0.1:$(docker_port_value "$root" PORT_SKYWALKING_OAP_GRPC 11800)"
  fi

  skywalking_enable_optional_plugins "$resolved_agent_path"
  export SKYWALKING_AGENT_PATH="$resolved_agent_path"
  export SKYWALKING_COLLECTOR_BACKEND_SERVICE="$resolved_backend"
  export SKYWALKING_AGENT_VERSION="$version"
  echo "SKYWALKING enabled=true agent=$resolved_agent_path backend=$resolved_backend version=$version"
  return 0
}
