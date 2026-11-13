#!/usr/bin/env bash
# Prepara o cliente ADB em Python. Não instala nada na TV.
set -euo pipefail
cd "$(dirname "$0")"

python3 -m venv tools/venv
tools/venv/bin/python -m pip install -q --upgrade pip
tools/venv/bin/python -m pip install -q -r tools/requirements.txt
echo "Cliente ADB Python pronto em tools/venv."

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$sdk" ] && [ -d "$HOME/Library/Android/sdk" ]; then
  sdk="$HOME/Library/Android/sdk"
fi
if [ -z "$sdk" ] && [ -d "$HOME/Android/Sdk" ]; then
  sdk="$HOME/Android/Sdk"
fi
if [ -n "$sdk" ] && [ ! -f local.properties ]; then
  printf 'sdk.dir=%s\n' "$sdk" > local.properties
  echo "local.properties apontando para $sdk"
elif [ ! -f local.properties ]; then
  echo "Para compilar, instale o Android SDK 34 e crie local.properties com sdk.dir=..."
  echo "O README explica o passo a passo."
fi
