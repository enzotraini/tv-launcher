#!/usr/bin/env bash
# Reabilita a lista, o setupwraith e o launcher do sistema, e devolve o Home para ele.
# Não desinstala o Para você. Se quiser removê-lo, faça isso depois que o Home antigo voltar.
# Uso: tools/restore.sh [--dry-run] examples/tcl-google-tv/debloat.txt
#
# Google TV:  STOCK_HOME padrão já aponta para o launcherx.
# Android TV antigo:
#   STOCK_HOME=com.google.android.tvlauncher/.MainActivity tools/restore.sh <lista>
set -euo pipefail
source "$(dirname "$0")/common.sh"
cd "$(repo_root)"

dry=0
if [ "${1:-}" = "--dry-run" ]; then
  dry=1
  shift
fi
LIST="${1:?Uso: tools/restore.sh [--dry-run] <lista>}"
PY="$(python_bin)"
STOCK_HOME="${STOCK_HOME:-com.google.android.apps.tv.launcherx/.home.HomeActivity}"
SETUPWRAITH_PACKAGE="${SETUPWRAITH_PACKAGE:-com.google.android.tungsten.setupwraith}"
if [ "$dry" -eq 1 ]; then
  "$PY" tools/debloat.py restore-commands "$LIST" \
    --stock-home "$STOCK_HOME" \
    --setupwraith "$SETUPWRAITH_PACKAGE"
  exit 0
fi
"$PY" tools/debloat.py restore-commands "$LIST" \
  --stock-home "$STOCK_HOME" \
  --setupwraith "$SETUPWRAITH_PACKAGE" \
  | "$PY" tools/tv.py --batch
echo "Restaurado. Aperte Home no controle."
echo "O app Para você continua instalado. Para removê-lo, com o Home antigo já ativo:"
echo "  $PY tools/tv.py 'pm uninstall dev.enzotraini.tvlauncher'"
