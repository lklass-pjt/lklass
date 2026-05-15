#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"

cat <<'HEADER'
============================================
LKLASS ASSIGNMENT RULE REFRESH
============================================
HEADER

for file in "$ROOT_DIR"/.agents/rules/*.md; do
  echo
  echo "----- ${file#$ROOT_DIR/} -----"
  sed -n '1,220p' "$file"
done

cat <<'FOOTER'
============================================
END RULE REFRESH
============================================
FOOTER

