#!/usr/bin/env python3
"""Cliente ADB por Wi-Fi. Não precisa do binário adb, mas usa o binário se o Python falhar.

Um comando de shell por chamada. Vários `pm` na mesma linha (`pm a; pm b`) podem
parar no primeiro sem avisar.

Abordagem adaptada de tv-home (MIT), © 2026 Mihir Gupta:
https://github.com/mihirgupta0900/tv-home
"""

from __future__ import annotations

import base64
import hashlib
import os
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
HOST_FILE = os.path.join(HERE, ".tv_host")
KEY = os.path.join(HERE, "adbkey", "adbkey")


def as_text(value: object) -> str:
    if isinstance(value, bytes):
        return value.decode("utf-8", "replace")
    return "" if value is None else str(value)


def resolve_endpoint() -> tuple[str, int]:
    host = os.environ.get("TV_HOST", "").strip()
    if not host and os.path.exists(HOST_FILE):
        with open(HOST_FILE, encoding="utf-8") as handle:
            host = handle.read().strip()
    if not host:
        raise SystemExit(
            "Não sei o IP da TV. Rode tools/find-tv.sh ou exporte TV_HOST=192.168.x.y"
        )
    port = int(os.environ.get("TV_PORT", "5555"))
    return host, port


class PythonAdb:
    name = "adb-shell"

    def __init__(self, device: object) -> None:
        self._device = device

    @classmethod
    def connect(cls, host: str, port: int) -> "PythonAdb":
        from adb_shell.adb_device import AdbDeviceTcp
        from adb_shell.auth.keygen import keygen
        from adb_shell.auth.sign_pythonrsa import PythonRSASigner

        os.makedirs(os.path.dirname(KEY), exist_ok=True)
        if not os.path.exists(KEY):
            keygen(KEY)
            os.chmod(KEY, 0o600)
        signer = PythonRSASigner(open(KEY + ".pub").read(), open(KEY).read())
        device = AdbDeviceTcp(host, port, default_transport_timeout_s=120.0)
        device.connect(rsa_keys=[signer], auth_timeout_s=120)
        return cls(device)

    def shell(self, command: str, timeout: float = 120) -> str:
        return as_text(self._device.shell(command, read_timeout_s=timeout))

    def push(self, local: str, remote: str) -> None:
        with open(local, "rb") as handle:
            data = handle.read()
        encoded = base64.b64encode(data).decode("ascii")
        temporary = remote + ".b64"
        self.shell(f"rm -f {temporary} {remote}")
        chunk = 48_000
        for start in range(0, len(encoded), chunk):
            piece = encoded[start : start + chunk]
            self.shell(f"echo -n '{piece}' >> {temporary}", timeout=180)
        self.shell(f"base64 -d {temporary} > {remote} && rm -f {temporary}", timeout=180)
        remote_md5 = self.shell(f"md5sum {remote}").split()[0]
        local_md5 = hashlib.md5(data).hexdigest()
        if remote_md5 != local_md5:
            raise SystemExit(f"md5 diferente: {remote_md5} != {local_md5}")
        print(f"enviado {local} -> {remote} ({len(data)} bytes, md5 ok)")

    def close(self) -> None:
        self._device.close()


class BinaryAdb:
    name = "adb"

    def __init__(self, serial: str) -> None:
        self.serial = serial

    @classmethod
    def connect(cls, host: str, port: int) -> "BinaryAdb":
        serial = f"{host}:{port}"
        subprocess.run(["adb", "start-server"], check=False, timeout=30)
        proc = subprocess.run(
            ["adb", "connect", serial],
            capture_output=True,
            text=True,
            timeout=30,
        )
        text = ((proc.stdout or "") + (proc.stderr or "")).strip()
        lowered = text.lower()
        if proc.returncode != 0 or "failed" in lowered or "cannot" in lowered or "refused" in lowered:
            raise RuntimeError(text or "adb connect falhou")
        return cls(serial)

    def shell(self, command: str, timeout: float = 120) -> str:
        proc = subprocess.run(
            ["adb", "-s", self.serial, "shell", command],
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        out = proc.stdout or ""
        err = proc.stderr or ""
        if err and err not in out:
            return out + err
        return out

    def push(self, local: str, remote: str) -> None:
        proc = subprocess.run(
            ["adb", "-s", self.serial, "push", local, remote],
            capture_output=True,
            text=True,
            timeout=180,
        )
        if proc.returncode != 0:
            raise SystemExit(((proc.stdout or "") + (proc.stderr or "")).strip())
        print(f"enviado {local} -> {remote}")

    def close(self) -> None:
        return None


def connect():
    mode = os.environ.get("ADB_CLIENT", "auto").strip().lower()
    host, port = resolve_endpoint()
    errors: list[str] = []
    if mode in ("auto", "python", "adb-shell"):
        try:
            client = PythonAdb.connect(host, port)
            print(f"ADB via adb-shell em {host}:{port}", file=sys.stderr)
            return client
        except Exception as exc:  # noqa: BLE001 - fallback consciente para o binário
            errors.append(f"adb-shell: {exc}")
            if mode != "auto":
                raise SystemExit("\n".join(errors)) from exc
    if mode in ("auto", "binary", "adb"):
        if shutil.which("adb"):
            try:
                client = BinaryAdb.connect(host, port)
                print(f"ADB via binário adb em {host}:{port}", file=sys.stderr)
                return client
            except Exception as exc:  # noqa: BLE001
                errors.append(f"adb: {exc}")
        else:
            errors.append("adb: binário não está no PATH")
    raise SystemExit("Não conectei na TV.\n" + "\n".join(errors))


def probe(client) -> None:
    props = (
        "ro.product.manufacturer",
        "ro.product.brand",
        "ro.product.model",
        "ro.product.device",
        "ro.build.version.release",
        "ro.build.version.sdk",
        "ro.build.display.id",
    )
    for prop in props:
        print(f"{prop} = {client.shell(f'getprop {prop}').strip()}")


def batch(client) -> None:
    for raw in sys.stdin:
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        print(f"$ {line}")
        print(client.shell(line, timeout=180).rstrip())


def main(argv: list[str]) -> int:
    args = argv[1:]
    client = connect()
    try:
        if not args or args == ["--probe"]:
            probe(client)
        elif args[0] == "--batch":
            batch(client)
        elif args[0] == "--push":
            if len(args) != 3:
                raise SystemExit("Uso: tools/tv.py --push <arquivo-local> <caminho-na-tv>")
            if not os.path.isfile(args[1]):
                raise SystemExit(f"arquivo não encontrado: {args[1]}")
            client.push(args[1], args[2])
        else:
            print(client.shell(" ".join(args), timeout=180).rstrip())
    finally:
        client.close()
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
