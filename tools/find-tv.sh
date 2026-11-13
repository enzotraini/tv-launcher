#!/usr/bin/env bash
# Acha a TV na rede e grava tools/.tv_host.
set -euo pipefail
source "$(dirname "$0")/common.sh"
ROOT="$(repo_root)"
cd "$ROOT"

if command -v dns-sd >/dev/null 2>&1; then
  echo "TVs anunciando Android TV Remote:"
  (dns-sd -B _androidtvremote2._tcp local. & pid=$!; sleep 3; kill "$pid" 2>/dev/null || true) 2>/dev/null \
    | awk '/Add/ {for (i=7;i<=NF;i++) printf "%s ", $i; print ""}' || true
elif command -v avahi-browse >/dev/null 2>&1; then
  echo "TVs anunciando Android TV Remote:"
  avahi-browse -rpt -t _androidtvremote2._tcp 2>/dev/null | head -n 20 || true
fi

exec "$(python_bin)" "$ROOT/tools/find_tv.py" "$@"
