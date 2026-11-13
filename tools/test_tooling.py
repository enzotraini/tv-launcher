#!/usr/bin/env python3
"""Checagens do debloat e de que o repositório não leva segredo nem rede."""

from __future__ import annotations

import os
import re
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(__file__))

import debloat
import find_tv

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
LIST = os.path.join(ROOT, "examples", "tcl-google-tv", "debloat.txt")
SETUP = "com.google.android.tungsten.setupwraith"


def assert_single_action(testcase: unittest.TestCase, commands: list[str]) -> None:
    for line in commands:
        testcase.assertNotIn(";", line)
        testcase.assertNotIn("&&", line)
        testcase.assertNotIn("uninstall", line.lower())
        testcase.assertLessEqual(line.lower().count("pm "), 1)


class DebloatTests(unittest.TestCase):
    def test_example_list_disables_without_touching_home_hijackers(self) -> None:
        packages = debloat.parse_list(LIST)
        commands = debloat.disable_commands(packages)
        self.assertEqual(len(commands), len(packages))
        blob = "\n".join(commands)
        self.assertNotIn(SETUP, blob)
        self.assertNotIn("launcherx", blob)
        self.assertNotIn("tvlauncher", blob)
        assert_single_action(self, commands)
        self.assertTrue(all(line.startswith("pm disable-user --user 0 ") for line in commands))

    def test_install_disables_setupwraith_after_setting_home(self) -> None:
        commands = debloat.install_commands()
        assert_single_action(self, commands)
        wraith = f"pm disable-user --user 0 {SETUP}"
        self.assertEqual(commands.count(wraith), 1)
        home = "cmd package set-home-activity --user 0 dev.enzotraini.tvlauncher/.HomeActivity"
        self.assertLess(commands.index(home), commands.index(wraith))
        self.assertTrue(commands[0].startswith("pm install -r "))
        self.assertIn("pm disable-user --user 0 com.google.android.apps.tv.launcherx", commands)

    def test_restore_reenables_setupwraith_and_stock_home(self) -> None:
        commands = debloat.restore_commands(debloat.parse_list(LIST), debloat.DEFAULT_STOCK_HOME)
        assert_single_action(self, commands)
        self.assertIn(f"pm enable {SETUP}", commands)
        self.assertIn("pm enable com.google.android.apps.tv.launcherx", commands)
        self.assertIn(
            "cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity",
            commands,
        )

    def test_rejects_uninstall_chain_and_reserved_packages(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            chained = os.path.join(tmp, "chain.txt")
            with open(chained, "w", encoding="utf-8") as handle:
                handle.write("com.tcl.ttvs; pm disable-user com.tcl.guard\n")
            with self.assertRaises(SystemExit):
                debloat.parse_list(chained)

            removal = os.path.join(tmp, "remove.txt")
            with open(removal, "w", encoding="utf-8") as handle:
                handle.write("pm uninstall com.tcl.ttvs\n")
            with self.assertRaises(SystemExit):
                debloat.parse_list(removal)

            reserved = os.path.join(tmp, "reserved.txt")
            with open(reserved, "w", encoding="utf-8") as handle:
                handle.write(SETUP + "\n")
            with self.assertRaises(SystemExit):
                debloat.parse_list(reserved)

    def test_remote_apk_rejects_shell_metacharacters(self) -> None:
        with self.assertRaises(SystemExit):
            debloat.install_commands(remote_apk="/data/local/tmp/x.apk; pm uninstall foo")

    def test_comments_are_ignored(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "list.txt")
            with open(path, "w", encoding="utf-8") as handle:
                handle.write("# com.google.android.tungsten.setupwraith\n")
                handle.write("com.tcl.ttvs # telemetria\n")
                handle.write("\n")
                handle.write("com.tcl.guard\n")
            self.assertEqual(debloat.parse_list(path), ["com.tcl.ttvs", "com.tcl.guard"])


class RepoTests(unittest.TestCase):
    def test_subnet_prefix(self) -> None:
        self.assertEqual(find_tv.subnet_prefix("192.168.15.20"), "192.168.15")

    def test_internet_permission_and_home_intent(self) -> None:
        manifest = open(os.path.join(ROOT, "app/src/main/AndroidManifest.xml"), encoding="utf-8").read()
        self.assertIn("android.permission.INTERNET", manifest)
        self.assertNotIn("android.permission.QUERY_ALL_PACKAGES", manifest)
        self.assertIn("android.intent.category.HOME", manifest)
        self.assertIn("android.intent.category.LEANBACK_LAUNCHER", manifest)
        self.assertIn("android.service.dreams.DreamService", manifest)

    def test_dock_order(self) -> None:
        xml = open(os.path.join(ROOT, "app/src/main/res/values/config.xml"), encoding="utf-8").read()
        block = re.search(r'name="dock_apps"[\s\S]*?</string-array>', xml)
        self.assertIsNotNone(block)
        packages = re.findall(r"<item>([^<]+)</item>", block.group(0))
        self.assertEqual(
            [item.strip() for item in packages],
            [
                "com.netflix.ninja",
                "com.google.android.youtube.tv",
                "com.globo.globotv",
                "com.amazon.amazonvideo.livingroom",
                "com.disney.disneyplus",
                "com.stremio.one",
            ],
        )

    def test_secrets_stay_out_of_git(self) -> None:
        gitignore = open(os.path.join(ROOT, ".gitignore"), encoding="utf-8").read()
        for entry in (
            "recommendations.properties",
            "tools/.tv_host",
            "tools/adbkey",
            "local.properties",
            "tools/venv/",
            "wallpapers/",
        ):
            self.assertIn(entry, gitignore)
        example = open(os.path.join(ROOT, "recommendations.properties.example"), encoding="utf-8").read()
        self.assertRegex(example, r"(?m)^tmdb\.api\.key=\s*$")
        gradle = open(os.path.join(ROOT, "app/build.gradle.kts"), encoding="utf-8").read()
        self.assertIn('applicationId = "dev.enzotraini.tvlauncher"', gradle)
        banned = (
            "Horizonte Perdido",
            "Horizonte Desconhecido",
            "Luz de Inverno",
            "Noite na Estação",
            "Hora do Faro",
            "Estrela Cadente",
            "Além do Sinal",
        )
        main = os.path.join(ROOT, "app", "src", "main")
        for dirpath, _, filenames in os.walk(main):
            for filename in filenames:
                if not filename.endswith((".kt", ".xml")):
                    continue
                with open(os.path.join(dirpath, filename), encoding="utf-8") as handle:
                    text = handle.read()
                for title in banned:
                    self.assertNotIn(title, text, filename)


if __name__ == "__main__":
    unittest.main()
