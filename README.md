# AEM JUnit agent (demo project)

Generic sample AEM-style project (no proprietary code) used to demo a Claude Code
skill + hook that writes unit tests for new Sling Models, OSGi services and servlets.

- `.claude/hooks/detect_new_java_class.py` : PostToolUse hook; fires when a new class under
  `src/main/java` has no mirrored `*Test.java`.
- `.claude/skills/aem-junit-agent/SKILL.md` : classify, draft, ask to save, ask to run, repair (max 2 loops).
- `src/test/java/...` : reference tests (the shape the agent should produce).

Try it: open this folder in Claude Code, ask it to create a new class under `src/main/java`
(e.g. a Sling Model), and watch the agent offer a test. Needs Java 11+, Maven, and access to Maven Central.
