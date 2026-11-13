# Funções compartilhadas pelos scripts. Não rode este arquivo direto.
repo_root() {
  cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd
}

python_bin() {
  local root
  root="$(repo_root)"
  if [ -x "$root/tools/venv/bin/python" ]; then
    printf '%s\n' "$root/tools/venv/bin/python"
  else
    printf '%s\n' python3
  fi
}
