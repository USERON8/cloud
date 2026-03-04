#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

MAVEN_VERSION="${MAVEN_VERSION:-3.9.9}"
MAVEN_BASE_DIR="${ROOT_DIR}/.tmp/tools"
MAVEN_HOME="${MAVEN_BASE_DIR}/apache-maven-${MAVEN_VERSION}"
MAVEN_BIN="${MAVEN_HOME}/bin/mvn"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

mkdir -p "${MAVEN_BASE_DIR}"

if [ ! -x "${MAVEN_BIN}" ]; then
  TMP_TAR="${MAVEN_BASE_DIR}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "${MAVEN_URL}" -o "${TMP_TAR}"
  elif command -v wget >/dev/null 2>&1; then
    wget -qO "${TMP_TAR}" "${MAVEN_URL}"
  else
    echo "Neither curl nor wget is available to download Maven." >&2
    exit 1
  fi

  tar -xzf "${TMP_TAR}" -C "${MAVEN_BASE_DIR}"
  rm -f "${TMP_TAR}"
fi

cd "${ROOT_DIR}"
exec "${MAVEN_BIN}" "$@"
