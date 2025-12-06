#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="${ROOT_DIR}/sim"
BUILD_DIR="${SRC_DIR}/build"

rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}"

javac -d "${BUILD_DIR}" "${SRC_DIR}"/*.java
java -cp "${BUILD_DIR}" Simulator "$@"
