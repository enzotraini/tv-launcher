#!/usr/bin/env bash
# Desabilita cada pacote da lista para o usuário 0. Não desinstala.
# Um pm por chamada. Desfazer: tools/restore.sh <a mesma lista>
# Uso: tools/disable.sh [--dry-run] examples/tcl-google-tv/debloat.txt
set -euo pipefail
source "$(dirname "$0")/common.sh"
cd "$(repo_root)"

dry=0
if [ "${1:-}" = "--dry-run" ]; then
  dry=1
  shift
fi
LIST="${1:?Uso: tools/disable.sh [--dry-run] <lista>}"
PY="$(python_bin)"
if [ "$dry" -eq 1 ]; then
  "$PY" tools/debloat.py disable-commands "$LIST"
  exit 0
fi
"$PY" tools/debloat.py disable-commands "$LIST" | "$PY" tools/tv.py --batch
echo "Pacotes desabilitados para o usuário 0. Nada foi desinstalado."
echo "Para voltar: tools/restore.sh $LIST"
