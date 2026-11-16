#!/usr/bin/env python3
"""PostToolUse hook: when Claude writes a new production Java class that has no
unit test yet, nudge Claude to run the aem-test-gate skill.

Reads the hook payload from stdin, prints a JSON hookSpecificOutput on match,
and stays silent (exit 0) otherwise. Pure stdlib so it runs anywhere.
"""
import json
import re
import sys
from pathlib import Path

MAIN = "src/main/java/"
TEST = "src/test/java/"


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except json.JSONDecodeError:
        return 0

    file_path = (payload.get("tool_input") or {}).get("file_path", "")
    norm = file_path.replace("\\", "/")
    if not norm.endswith(".java") or MAIN not in norm:
        return 0
    if norm.endswith(("package-info.java", "module-info.java")):
        return 0

    src = Path(file_path)
    test_path = Path(norm.replace(MAIN, TEST)[: -len(".java")] + "Test.java")
    if test_path.exists():
        return 0  # already has a test

    text = src.read_text(encoding="utf-8", errors="ignore") if src.exists() else ""
    # Skip pure interfaces/enums/annotations: nothing executable to test.
    if re.search(r"\b(public\s+)?(interface|@interface|enum)\s+\w+", text) and not re.search(r"\bclass\s+\w+", text):
        return 0

    msg = (
        f"A new AEM Java class was created at {file_path} and has no unit test "
        f"(expected {test_path}). Use the aem-test-gate skill to generate one. "
        "Follow the skill: classify the class, draft the test, show it to the user, "
        "and ask for permission before saving and before running mvn test."
    )
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "PostToolUse", "additionalContext": msg}}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
