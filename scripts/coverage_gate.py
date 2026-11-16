#!/usr/bin/env python3
"""Read JaCoCo's CSV report and fail if line coverage is below a threshold.

Pure standard library. Run after `mvn test` (the pom binds jacoco:report to the
test phase, so target/site/jacoco/jacoco.csv is written automatically):

    python3 scripts/coverage_gate.py --min 60
    python3 scripts/coverage_gate.py --class HeroModel

Exists so the agent can answer "did the test it just wrote actually cover anything?"
rather than reporting that the build was green.
"""
from __future__ import annotations

import argparse
import csv
import sys
from pathlib import Path

DEFAULT_CSV = "target/site/jacoco/jacoco.csv"


def pct(covered: int, missed: int) -> float:
    total = covered + missed
    return 100.0 * covered / total if total else 0.0


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--report", default=DEFAULT_CSV, help=f"path to jacoco.csv (default: {DEFAULT_CSV})")
    ap.add_argument("--min", type=float, default=None, help="fail if overall line coverage is under this percent")
    ap.add_argument("--class", dest="only", default=None, help="report a single class by simple name")
    args = ap.parse_args()

    report = Path(args.report)
    if not report.exists():
        print(f"No JaCoCo report at {report}. Run `mvn clean test` first.", file=sys.stderr)
        return 2

    with report.open(newline="", encoding="utf-8") as fh:
        rows = list(csv.DictReader(fh))

    if args.only:
        rows = [r for r in rows if r["CLASS"] == args.only or r["CLASS"].endswith("." + args.only)]
        if not rows:
            print(f"Class {args.only} is not in the report. It may have no tests at all.", file=sys.stderr)
            return 2

    line_cov = sum(int(r["LINE_COVERED"]) for r in rows)
    line_miss = sum(int(r["LINE_MISSED"]) for r in rows)
    br_cov = sum(int(r["BRANCH_COVERED"]) for r in rows)
    br_miss = sum(int(r["BRANCH_MISSED"]) for r in rows)

    scored = sorted(
        ((r["CLASS"], pct(int(r["LINE_COVERED"]), int(r["LINE_MISSED"]))) for r in rows),
        key=lambda t: t[1],
    )
    width = max(len(name) for name, _ in scored)
    print(f"{'CLASS'.ljust(width)}  LINE %")
    print("-" * (width + 8))
    for name, value in scored:
        print(f"{name.ljust(width)}  {value:6.1f}")

    overall = pct(line_cov, line_miss)
    print(f"\nOverall line coverage: {overall:.1f}%  ({line_cov} covered, {line_miss} missed)")
    if br_cov + br_miss:
        print(f"Branch coverage:       {pct(br_cov, br_miss):.1f}%")

    if args.min is not None and overall < args.min:
        print(f"\nFAIL: below the {args.min:.1f}% threshold.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
