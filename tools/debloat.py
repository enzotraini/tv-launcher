#!/usr/bin/env python3
"""Gera comandos para a TV, um por linha. Nunca desinstala.

O fluxo (desabilitar, restaurar, e tirar o setupwraith do caminho do Home)
foi adaptado de tv-home (MIT), © 2026 Mihir Gupta:
https://github.com/mihirgupta0900/tv-home
"""

from __future__ import annotations

import argparse
import re
import sys

SETUPWRAITH = "com.google.android.tungsten.setupwraith"
DEFAULT_STOCK_PACKAGE = "com.google.android.apps.tv.launcherx"
DEFAULT_STOCK_HOME = "com.google.android.apps.tv.launcherx/.home.HomeActivity"
LAUNCHER_COMPONENT = "dev.enzotraini.tvlauncher/.HomeActivity"
REMOTE_APK = "/data/local/tmp/para-voce.apk"

# O launcher do sistema e o setupwraith não entram na lista comum.
# install-launcher.sh desabilita os dois; restore.sh liga os dois de novo.
RESERVED = {
    SETUPWRAITH,
    DEFAULT_STOCK_PACKAGE,
    "com.google.android.tvlauncher",
}

PKG_RE = re.compile(r"^[A-Za-z]\w*(?:\.[A-Za-z_]\w*)+$")
COMPONENT_RE = re.compile(r"^[A-Za-z][\w.]*/\.?[A-Za-z][\w.$]*$")
REMOTE_APK_RE = re.compile(r"^/[A-Za-z0-9_./-]+$")


def parse_list(path: str) -> list[str]:
    packages: list[str] = []
    seen: set[str] = set()
    with open(path, encoding="utf-8") as handle:
        for lineno, raw in enumerate(handle, 1):
            line = raw.split("#", 1)[0].strip()
            if not line:
                continue
            if any(token in line for token in (";", "&&", "|", "`")) or "uninstall" in line.lower():
                raise SystemExit(
                    f"{path}:{lineno}: só um nome de pacote por linha. "
                    "Não use uninstall nem encadeie comandos."
                )
            token = line.split()[0]
            if not PKG_RE.fullmatch(token):
                raise SystemExit(f"{path}:{lineno}: pacote inválido: {token}")
            if token in RESERVED:
                raise SystemExit(
                    f"{path}:{lineno}: {token} não entra na lista de debloat. "
                    "tools/install-launcher.sh desabilita o launcher do sistema e o "
                    "setupwraith; tools/restore.sh liga os dois de novo."
                )
            if token not in seen:
                seen.add(token)
                packages.append(token)
    if not packages:
        raise SystemExit(f"{path}: lista vazia")
    return packages


def require_package(value: str) -> str:
    if not PKG_RE.fullmatch(value):
        raise SystemExit(f"pacote inválido: {value}")
    return value


def require_component(value: str) -> str:
    if not COMPONENT_RE.fullmatch(value):
        raise SystemExit(f"componente inválido: {value}")
    return value


def disable_commands(packages: list[str]) -> list[str]:
    return [f"pm disable-user --user 0 {package}" for package in packages]


def restore_commands(
    packages: list[str],
    stock_home: str,
    setupwraith: str = SETUPWRAITH,
) -> list[str]:
    stock_home = require_component(stock_home)
    setupwraith = require_package(setupwraith)
    stock_package = stock_home.split("/", 1)[0]
    commands = [f"pm enable {package}" for package in packages]
    for extra in (setupwraith, stock_package):
        if extra not in packages:
            commands.append(f"pm enable {extra}")
    launcher_package = LAUNCHER_COMPONENT.split("/", 1)[0]
    commands.append(f"cmd package set-home-activity --user 0 {stock_home}")
    commands.append(f"am force-stop {launcher_package}")
    commands.append("input keyevent KEYCODE_HOME")
    return commands


def install_commands(
    component: str = LAUNCHER_COMPONENT,
    stock_package: str = DEFAULT_STOCK_PACKAGE,
    remote_apk: str = REMOTE_APK,
    setupwraith: str = SETUPWRAITH,
) -> list[str]:
    component = require_component(component)
    stock_package = require_package(stock_package)
    setupwraith = require_package(setupwraith)
    if not REMOTE_APK_RE.fullmatch(remote_apk):
        raise SystemExit(f"caminho remoto inválido: {remote_apk}")
    launcher_package = component.split("/", 1)[0]
    return [
        f"pm install -r {remote_apk}",
        f"rm -f {remote_apk}",
        f"cmd package set-home-activity --user 0 {component}",
        f"pm disable-user --user 0 {stock_package}",
        f"pm disable-user --user 0 {setupwraith}",
        f"cmd package set-home-activity --user 0 {component}",
        "am force-stop com.android.tv.settings",
        f"am force-stop {launcher_package}",
        "input keyevent KEYCODE_HOME",
    ]


def _print_lines(lines: list[str]) -> None:
    for line in lines:
        print(line)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Um comando pm por linha. Nunca uninstall.")
    sub = parser.add_subparsers(dest="cmd", required=True)

    disable = sub.add_parser("disable-commands")
    disable.add_argument("list")

    restore = sub.add_parser("restore-commands")
    restore.add_argument("list")
    restore.add_argument("--stock-home", default=DEFAULT_STOCK_HOME)
    restore.add_argument("--setupwraith", default=SETUPWRAITH)

    install = sub.add_parser("install-commands")
    install.add_argument("--component", default=LAUNCHER_COMPONENT)
    install.add_argument("--stock-package", default=DEFAULT_STOCK_PACKAGE)
    install.add_argument("--remote-apk", default=REMOTE_APK)
    install.add_argument("--setupwraith", default=SETUPWRAITH)

    sub.add_parser("remote-apk")

    check = sub.add_parser("check")
    check.add_argument("list")

    args = parser.parse_args(argv)
    if args.cmd == "disable-commands":
        _print_lines(disable_commands(parse_list(args.list)))
    elif args.cmd == "restore-commands":
        _print_lines(
            restore_commands(parse_list(args.list), args.stock_home, args.setupwraith)
        )
    elif args.cmd == "install-commands":
        _print_lines(
            install_commands(
                args.component,
                args.stock_package,
                args.remote_apk,
                args.setupwraith,
            )
        )
    elif args.cmd == "remote-apk":
        print(REMOTE_APK)
    elif args.cmd == "check":
        parse_list(args.list)
        print("ok")
    return 0


if __name__ == "__main__":
    sys.exit(main())
