#!/usr/bin/env bash
# Grava modelo, pacotes e o Home atual em backup/<data>/. Rode antes do debloat.
set -euo pipefail
source "$(dirname "$0")/common.sh"
cd "$(repo_root)"
PY="$(python_bin)"
stamp="$(date +%Y%m%d-%H%M%S)"
dir="backup/$stamp"
mkdir -p "$dir"
"$PY" tools/tv.py --probe | tee "$dir/device.txt"
{
  echo "pm list packages -f"
  echo "pm list packages -d"
  echo "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
} | "$PY" tools/tv.py --batch | tee "$dir/packages.txt"
echo "Backup em $dir"
