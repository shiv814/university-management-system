#!/usr/bin/env python3
"""Cross-platform Java build and test runner used by GitHub Actions."""

from __future__ import annotations

import shutil
import subprocess
from pathlib import Path


def run(command: list[str]) -> None:
    print("+", " ".join(command), flush=True)
    subprocess.run(command, check=True)


def sources(root: Path) -> list[str]:
    return [str(path) for path in sorted(root.rglob("*.java"))]


def main() -> None:
    project = Path(__file__).resolve().parents[1]
    build = project / "build"
    classes = build / "classes"
    tests = build / "test-classes"

    shutil.rmtree(build, ignore_errors=True)
    classes.mkdir(parents=True)
    tests.mkdir(parents=True)

    main_sources = sources(project / "src" / "main" / "java")
    test_sources = sources(project / "src" / "test" / "java")
    if not main_sources or not test_sources:
        raise SystemExit("Expected both production and test Java sources")

    run(["javac", "-Xlint:all", "-Werror", "-d", str(classes), *main_sources])
    run(["javac", "-Xlint:all", "-Werror", "-cp", str(classes), "-d", str(tests), *test_sources])
    separator = ";" if __import__("os").name == "nt" else ":"
    classpath = f"{classes}{separator}{tests}"
    run(["java", "-ea", "-cp", classpath, "ca.shivam.university.TestRunner"] )


if __name__ == "__main__":
    main()
