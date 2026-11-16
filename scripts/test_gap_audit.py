#!/usr/bin/env python3
"""Report which AEM production classes have no unit test, ranked by risk.

Pure standard library. Works on any Maven-layout AEM project, with or without an agent:

    python3 scripts/test_gap_audit.py
    python3 scripts/test_gap_audit.py --root core --json

Risk is a crude proxy: branch-ish keywords in the class. A 200-line service with
twelve branches and no test outranks a three-line model with none.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

MAIN = "src/main/java"
TEST = "src/test/java"

# Order matters: the first match wins.
KINDS = [
    ("workflow", re.compile(r"implements\s+[\w.]*WorkflowProcess\b"), "out of scope"),
    ("servlet", re.compile(r"extends\s+Sling(?:Safe|All)\w*Servlet\b|service\s*=\s*Servlet\.class"), "AemContext + mocked refs"),
    ("sling-model", re.compile(r"@Model\b"), "AemContext + addModelsForClasses"),
    ("osgi-service", re.compile(r"@Component\b"), "registerInjectActivateService"),
    ("scheduler", re.compile(r"@Designate\b"), "registerInjectActivateService"),
]

BRANCHY = re.compile(r"\b(if|for|while|case|catch)\s*[\(:]|\?\s*[^:]+:")
TYPE_DECL = re.compile(r"\b(class|interface|enum|@interface)\s+(\w+)")


def classify(text: str) -> tuple[str, str]:
    for name, pattern, scaffold in KINDS:
        if pattern.search(text):
            return name, scaffold
    return "plain-java", "plain JUnit 5"


def is_testable(text: str) -> bool:
    """Interfaces, enums and annotations on their own have nothing to execute."""
    decls = TYPE_DECL.findall(text)
    return any(kind == "class" for kind, _ in decls)


def scan(root: Path) -> list[dict]:
    rows = []
    for src in sorted(root.rglob("*.java")):
        norm = src.as_posix()
        if MAIN not in norm or src.name in ("package-info.java", "module-info.java"):
            continue
        text = src.read_text(encoding="utf-8", errors="ignore")
        if not is_testable(text):
            continue
        kind, scaffold = classify(text)
        test_path = Path(norm.replace(MAIN, TEST)[: -len(".java")] + "Test.java")
        rows.append(
            {
                "class": src.stem,
                "kind": kind,
                "scaffold": scaffold,
                "branches": len(BRANCHY.findall(text)),
                "lines": text.count("\n") + 1,
                "has_test": test_path.exists(),
                "source": norm,
                "expected_test": test_path.as_posix(),
            }
        )
    return rows


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=".", help="project root or module to scan (default: .)")
    ap.add_argument("--json", action="store_true", help="emit JSON instead of a table")
    ap.add_argument("--fail-on-gap", action="store_true", help="exit 1 if any class lacks a test")
    args = ap.parse_args()

    root = Path(args.root).resolve()
    if not root.exists():
        print(f"No such path: {root}", file=sys.stderr)
        return 2

    rows = scan(root)
    if not rows:
        print(f"No testable classes found under {root}/{MAIN}")
        return 0

    # Untested first, then the branchiest.
    rows.sort(key=lambda r: (r["has_test"], -r["branches"], r["class"]))
    gaps = [r for r in rows if not r["has_test"]]

    if args.json:
        print(json.dumps({"scanned": len(rows), "gaps": len(gaps), "classes": rows}, indent=2))
        return 1 if (gaps and args.fail_on_gap) else 0

    width = max(len(r["class"]) for r in rows)
    print(f"{'CLASS'.ljust(width)}  {'KIND':<13} {'BRANCHES':>8}  TEST")
    print("-" * (width + 36))
    for r in rows:
        mark = "yes" if r["has_test"] else "MISSING"
        print(f"{r['class'].ljust(width)}  {r['kind']:<13} {r['branches']:>8}  {mark}")

    print(f"\n{len(rows)} testable classes, {len(gaps)} without a test.")
    if gaps:
        print("\nStart here:")
        for r in gaps[:5]:
            print(f"  {r['class']} ({r['kind']}, {r['branches']} branches) -> {r['expected_test']}")
            print(f"    scaffold: {r['scaffold']}")
    workflow = [r for r in rows if r["kind"] == "workflow"]
    if workflow:
        print(f"\nNote: {len(workflow)} workflow process step(s) found; the agent treats these as out of scope.")

    return 1 if (gaps and args.fail_on_gap) else 0


if __name__ == "__main__":
    sys.exit(main())
