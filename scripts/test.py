#!/usr/bin/env python3
"""Cross-platform Java build/test/demo runner used by GitHub Actions."""
from __future__ import annotations

import os
import shutil
import subprocess
from pathlib import Path


def run(command: list[str], *, cwd: Path | None = None) -> None:
    print("+", " ".join(command), flush=True)
    subprocess.run(command, check=True, cwd=cwd)


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
    separator = ";" if os.name == "nt" else ":"
    classpath = f"{classes}{separator}{tests}"
    run(["java", "-ea", "-cp", classpath, "ca.shivam.university.TestRunner"], cwd=project)
    run(["java", "-ea", "-cp", classpath, "ca.shivam.university.AdvancedTestRunner"], cwd=project)
    run(["java", "-ea", "-cp", classpath, "ca.shivam.university.V3Demo"], cwd=project)
    report = build / "portfolio-report.html"
    if not report.exists() or report.stat().st_size < 2000:
        raise SystemExit("v3 demo did not produce a substantive HTML report")


if __name__ == "__main__":
    main()
