#!/usr/bin/env bash
# Compila o APK de debug, instala na TV e torna o Para você o Home.
# Também desabilita o launcher do Google TV e o setupwraith (ele rouba o botão Home).
#
#   tools/install-launcher.sh              compila e instala
#   tools/install-launcher.sh --build-only só gera o APK
#   tools/install-launcher.sh --dry-run    mostra os comandos, sem compilar e sem falar com a TV
#
# Android TV antigo, sem launcherx:
#   STOCK_PACKAGE=com.google.android.tvlauncher tools/install-launcher.sh
set -euo pipefail
source "$(dirname "$0")/common.sh"
cd "$(repo_root)"

mode="${1:-}"
PY="$(python_bin)"
STOCK_PACKAGE="${STOCK_PACKAGE:-com.google.android.apps.tv.launcherx}"
SETUPWRAITH_PACKAGE="${SETUPWRAITH_PACKAGE:-com.google.android.tungsten.setupwraith}"
COMPONENT="${LAUNCHER_COMPONENT:-dev.enzotraini.tvlauncher/.HomeActivity}"

if [ "$mode" = "--dry-run" ]; then
  "$PY" tools/debloat.py install-commands \
    --stock-package "$STOCK_PACKAGE" \
    --component "$COMPONENT" \
    --setupwraith "$SETUPWRAITH_PACKAGE"
  exit 0
fi

if [ ! -x ./gradlew ]; then
  echo "gradlew não encontrado na raiz do repositório."
  exit 1
fi
./gradlew :app:assembleDebug
APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
  echo "APK não foi gerado em $APK"
  exit 1
fi
echo "APK: $APK"
if [ "$mode" = "--build-only" ]; then
  exit 0
fi
if [ -n "$mode" ]; then
  echo "Opção desconhecida: $mode"
  exit 1
fi

REMOTE="$("$PY" tools/debloat.py remote-apk)"
"$PY" tools/tv.py --push "$APK" "$REMOTE"
"$PY" tools/debloat.py install-commands \
  --stock-package "$STOCK_PACKAGE" \
  --component "$COMPONENT" \
  --remote-apk "$REMOTE" \
  --setupwraith "$SETUPWRAITH_PACKAGE" \
  | "$PY" tools/tv.py --batch
echo "Instalado. Aperte Home no controle."
echo "Teste de novo o Home com um app aberto (Netflix, YouTube ou Configurações)."
