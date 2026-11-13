#!/usr/bin/env python3
"""Procura uma Android TV na rede local (porta 5555) e grava tools/.tv_host.

Uso:
  tools/find-tv.sh
  tools/find-tv.sh 192.168.0.20
"""

from __future__ import annotations

import os
import re
import socket
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor

HERE = os.path.dirname(os.path.abspath(__file__))
HOST_FILE = os.path.join(HERE, ".tv_host")
IP_RE = re.compile(r"^\d{1,3}(?:\.\d{1,3}){3}$")


def subnet_prefix(ip: str) -> str:
    parts = ip.split(".")
    if len(parts) != 4 or not all(part.isdigit() and 0 <= int(part) <= 255 for part in parts):
        raise ValueError(ip)
    return ".".join(parts[:3])


def valid_ip(ip: str) -> bool:
    if not IP_RE.fullmatch(ip):
        return False
    return all(0 <= int(part) <= 255 for part in ip.split("."))


def local_ipv4() -> str:
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.connect(("192.0.2.1", 9))
        ip = sock.getsockname()[0]
    finally:
        sock.close()
    if not ip or ip.startswith("127."):
        raise SystemExit("Não consegui achar o IP deste computador. Você está no Wi-Fi?")
    return ip


def port_open(host: str, port: int, timeout: float = 0.35) -> bool:
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(timeout)
    try:
        sock.connect((host, port))
        return True
    except OSError:
        return False
    finally:
        sock.close()


def cast_name(host: str) -> str:
    url = f"http://{host}:8008/setup/eureka_info?params=name"
    try:
        with urllib.request.urlopen(url, timeout=2) as response:
            raw = response.read().decode("utf-8", "replace")
    except Exception:
        return ""
    match = re.search(r'"name"\s*:\s*"([^"]+)"', raw)
    return match.group(1) if match else ""


def sort_hosts(hosts: list[str]) -> list[str]:
    return sorted(hosts, key=lambda host: int(host.split(".")[-1]))


def scan(prefix: str) -> tuple[list[str], list[str]]:
    hosts = [f"{prefix}.{index}" for index in range(1, 255)]

    def check(host: str) -> tuple[str, bool, bool]:
        return host, port_open(host, 5555), port_open(host, 8008)

    adb_hosts: list[str] = []
    cast_hosts: list[str] = []
    with ThreadPoolExecutor(max_workers=64) as pool:
        for host, adb, cast in pool.map(check, hosts):
            if adb:
                adb_hosts.append(host)
            if cast:
                cast_hosts.append(host)
    return sort_hosts(adb_hosts), sort_hosts(cast_hosts)


def save(host: str) -> None:
    if not valid_ip(host):
        raise SystemExit(f"IP inválido: {host}")
    with open(HOST_FILE, "w", encoding="utf-8") as handle:
        handle.write(host + "\n")


def main(argv: list[str]) -> int:
    if len(argv) == 2:
        save(argv[1])
        print(f"Salvei {argv[1]} em tools/.tv_host")
        print("Próximo passo: tools/venv/bin/python tools/tv.py --probe")
        return 0
    if len(argv) > 2:
        raise SystemExit("Uso: tools/find-tv.sh [192.168.x.y]")

    mine = local_ipv4()
    prefix = subnet_prefix(mine)
    print(f"Varrendo {prefix}.0/24 a partir de {mine} …")
    adb_hosts, cast_hosts = scan(prefix)
    for host in cast_hosts:
        name = cast_name(host)
        suffix = f" ({name})" if name else ""
        print(f"Aparelho Cast: {host}{suffix}")
    if not adb_hosts:
        print("Nenhuma TV com ADB na porta 5555.")
        print("Na TV: opções de desenvolvedor → depuração USB. Se aparecer depuração sem fio, anote a porta e exporte TV_PORT.")
        if cast_hosts:
            print("A TV provavelmente é um dos aparelhos Cast acima.")
        return 1
    print("ADB aberto em: " + " ".join(adb_hosts))
    chosen = adb_hosts[0]
    save(chosen)
    print(f"Salvei {chosen} em tools/.tv_host")
    if len(adb_hosts) > 1:
        print("Havia mais de um aparelho. Se não for a TV, rode tools/find-tv.sh com o IP certo.")
    print("Próximo passo: tools/venv/bin/python tools/tv.py --probe")
    print("Aceite “Permitir depuração USB?” na TV, se o aviso aparecer.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
