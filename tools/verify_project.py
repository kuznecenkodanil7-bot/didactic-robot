#!/usr/bin/env python3
"""Dependency-free structural validation for the source bundle."""
from __future__ import annotations

import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = [
    ROOT / "build.gradle",
    ROOT / "gradle.properties",
    ROOT / "src/main/resources/fabric.mod.json",
    ROOT / "src/main/resources/labychat.mixins.json",
    ROOT / "src/main/java/io/github/labychat/LabyChatClient.java",
]


def check_json_files() -> int:
    count = 0
    for path in sorted((ROOT / "src/main/resources").rglob("*.json")):
        text = path.read_text(encoding="utf-8").replace('"${version}"', '"0.1.0"')
        json.loads(text)
        count += 1
    return count


def check_png(path: Path) -> tuple[int, int]:
    raw = path.read_bytes()
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"Invalid PNG signature: {path}")
    width, height = struct.unpack(">II", raw[16:24])
    return width, height


def main() -> None:
    missing = [str(path.relative_to(ROOT)) for path in REQUIRED if not path.is_file()]
    if missing:
        raise SystemExit("Missing required files: " + ", ".join(missing))
    json_count = check_json_files()
    width, height = check_png(ROOT / "src/main/resources/assets/labychat/icon.png")
    java_count = len(list((ROOT / "src/main/java").rglob("*.java")))
    if java_count < 20:
        raise SystemExit(f"Unexpectedly small Java source set: {java_count}")
    print(f"OK: {java_count} Java files, {json_count} JSON files, icon {width}x{height}")


if __name__ == "__main__":
    main()
