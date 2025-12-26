#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$ROOT_DIR/src/main/java"
OUT_DIR="$ROOT_DIR/out"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

javac -d "$OUT_DIR" $(find "$SRC_DIR" -name '*.java')

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <source.asm>"
  exit 1
fi

java -cp "$OUT_DIR" assembler.Main "$@"
