#!/usr/bin/env bash
# Envia fotos para o armazenamento do app. Não pede permissão extra.
# A pasta wallpapers/ na raiz do repositório fica de fora do git.
#
# Uso, com a TV já pareada (o mesmo caminho de tools/tv.py):
#   tools/push-wallpapers.sh
#   tools/push-wallpapers.sh /caminho/das/fotos
#
# O app precisa estar instalado em debug. O script tenta a pasta externa
# Android/data/.../files/wallpapers e, se o shell não puder gravar lá,
# copia com run-as para files/wallpapers, que o app também lê.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${1:-$ROOT/wallpapers}"
PKG="dev.enzotraini.tvlauncher"
PY="${ROOT}/tools/venv/bin/python"
if [[ ! -x "$PY" ]]; then
  PY="$(command -v python3)"
fi
TV="${ROOT}/tools/tv.py"
REMOTE_TMP="/data/local/tmp/tvlauncher-wallpapers"

if [[ ! -d "$SRC" ]]; then
  echo "Pasta não encontrada: $SRC" >&2
  echo "Crie wallpapers/ na raiz (ela não entra no git) e coloque as fotos lá." >&2
  exit 1
fi

shopt -s nullglob
files=()
for path in "$SRC"/*; do
  [[ -f "$path" ]] || continue
  base="$(basename "$path")"
  if [[ ! "$base" =~ ^[A-Za-z0-9._-]+$ ]] || [[ "$base" == *..* ]]; then
    echo "Nome recusado: $base" >&2
    exit 1
  fi
  ext="${base##*.}"
  ext="$(printf '%s' "$ext" | tr '[:upper:]' '[:lower:]')"
  case "$ext" in
    jpg|jpeg|png|webp) files+=("$path") ;;
    *) echo "Ignorado: $base" >&2 ;;
  esac
done

if [[ ${#files[@]} -eq 0 ]]; then
  echo "Nenhuma foto jpg, png ou webp em $SRC" >&2
  exit 1
fi

shell() {
  "$PY" "$TV" "$1"
}

shell "mkdir -p ${REMOTE_TMP}"

sent=0
for path in "${files[@]}"; do
  base="$(basename "$path")"
  "$PY" "$TV" --push "$path" "${REMOTE_TMP}/${base}"
  external="$(shell "mkdir -p /sdcard/Android/data/${PKG}/files/wallpapers /storage/emulated/0/Android/data/${PKG}/files/wallpapers && cp ${REMOTE_TMP}/${base} /sdcard/Android/data/${PKG}/files/wallpapers/${base} && cp ${REMOTE_TMP}/${base} /storage/emulated/0/Android/data/${PKG}/files/wallpapers/${base} && echo WALLPAPER_OK" || true)"
  internal="$(shell "cat ${REMOTE_TMP}/${base} | run-as ${PKG} sh -c 'mkdir -p files/wallpapers && cat > files/wallpapers/${base}' && echo WALLPAPER_OK" || true)"
  if [[ "$external" != *WALLPAPER_OK* && "$internal" != *WALLPAPER_OK* ]]; then
    echo "Não consegui gravar ${base}. Instale o app de debug antes e confira o ADB." >&2
    echo "Externo: ${external}" >&2
    echo "Interno: ${internal}" >&2
    exit 1
  fi
  sent=$((sent + 1))
  echo "ok ${base}"
done

shell "rm -rf ${REMOTE_TMP}" || true
echo "${sent} foto(s) no armazenamento do app. Abra Fundos do descanso para girar e escolher."
