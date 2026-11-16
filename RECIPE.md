# Draft submission — Experience League AI Recipe Library

Paste the sections below into the *Share a recipe* form at
https://experienceleaguecommunities.adobe.com/ai-recipe-library-224

Before submitting: rename the GitHub repo to `aem-test-gate` (Settings → Repository name;
GitHub keeps redirects from the old URL), update the links below, and add your own notes
to [LESSONS] — that section is what makes a recipe read as practitioner-written.

---

**Title:** AEM Test Gate: make the agent run the test it just wrote

[PROBLEM]
Agent-written unit tests are cheap to produce and easy to over-trust. A test that was
generated but never executed tells you the model wrote something plausible, not that the
code works. A test can also pass while asserting almost nothing, and nothing in the normal
workflow surfaces that.

[OVERVIEW]
A Claude Code hook plus skill that closes the loop on AEM unit tests. When a new class
lands under `src/main/java` with no matching test, a PostToolUse hook notices and starts
the skill. The skill classifies the class (Sling Model, OSGi service, Sling servlet),
drafts a JUnit 5 test with AEM Mocks, asks permission before saving, asks again before
running, executes `./mvnw test`, repairs its own failures at most twice, and reports
JaCoCo line coverage for that class.

Two standalone Python scripts ship with it and need no agent at all:
`test_gap_audit.py` reports which classes have no test, ranked by branch count, and
`coverage_gate.py` fails a build under a line-coverage threshold.

This builds on two existing recipes rather than repeating them. Narendra Gandhi's
*AEM Junit Skill* covers template generation and works outside Claude Code — use that one
if templates are all you need. His *Use VS Code GitHub Copilot Hooks to Run Semgrep in an
AEM Project* establishes the PostToolUse hook pattern. What is added here is the
missing-test detection, the execute-and-repair loop, the coverage check, and two human
gates.

[IMPACT]
- Tests get written for classes that would otherwise ship untested, because the prompt
  arrives when the class is created rather than when someone remembers.
- The agent cannot claim success without evidence: it runs the test and reports coverage.
- Two gates keep a person in control of what lands on disk and what executes.
- The conventions live in git, so they are reviewed in pull requests and picked up by
  everyone who clones the repo, instead of living in one developer's chat history.
- `test_gap_audit.py` answers "what is untested right now" across a codebase in about a
  second, including classes a developer wrote by hand that no hook ever saw.
- [TODO: add your own numbers once you have run it on a real project.]

[LESSONS]
- **Pin `maven-compiler-plugin`.** A bare pom used the default plugin version, which
  ignored the `maven.compiler.release` property and compiled for Java 8. AEM Mocks 5.x
  ships Java 11 bytecode, so the build failed with
  `class file has wrong version 55.0, should be 52.0`.
- **OSGi Mock needs SCR metadata.** Without `bnd-maven-plugin` generating component
  metadata, `registerInjectActivateService` fails with `NoScrMetadata`. This is the single
  most confusing failure for anyone copying the pattern into a non-archetype project.
- **Assert the content type as AEM Mocks returns it.** The servlet test expected
  `application/json` and got `application/json;charset=UTF-8`.
- **The hook has to be fussy.** Firing on every write is noise. It fires only when the
  file is under `src/main/java`, has no mirrored `*Test.java`, and declares an actual
  class — interfaces, enums and annotations are skipped.
- **Cap the repair loop at two.** Beyond that the agent starts weakening assertions to
  make the test pass, which is the opposite of the point. After two it stops and reports,
  and it is told to flag a likely production bug rather than bend the test around it.
- **Make the hook portable.** `python3` does not exist on a default Windows install, so
  the hook command probes for `python3` and falls back to `python`.
- [TODO: add what surprised you on a real codebase.]

[TRYABLE]
```bash
git clone https://github.com/snbaem/aem-test-gate
cd aem-test-gate

# 1. Baseline. Needs JDK 11+; the wrapper pins Maven 3.9.9, same as CI.
./mvnw clean test            # Windows: .\mvnw.cmd clean test

# 2. The agent-free half, useful on any AEM project
python3 scripts/test_gap_audit.py            # ranked "no test" report
python3 scripts/coverage_gate.py --min 60    # coverage gate

# 3. Check the hook alone, no Claude needed
echo '{"tool_name":"Write","tool_input":{"file_path":"src/main/java/com/example/demo/models/NoTestYet.java"}}' \
  | python3 .claude/hooks/detect_new_java_class.py
```

Then open the folder in Claude Code, start a new session, and ask it to create a Sling
Model. The hook fires, the skill proposes a test, and it stops for you twice before
anything is saved or run. Saying no at either gate ends the flow there.

The hook is wired in `.claude/settings.json`:

```json
{
  "hooks": {
    "PostToolUse": [
      { "matcher": "Write",
        "hooks": [ { "type": "command",
                     "command": "if python3 -c '' >/dev/null 2>&1; then PY=python3; else PY=python; fi; \"$PY\" \"$CLAUDE_PROJECT_DIR/.claude/hooks/detect_new_java_class.py\"" } ] }
    ]
  }
}
```

[SETUP]
- JDK 11 or newer. On Java 8 the build fails with `invalid flag: --release`.
- Python 3 on `PATH` as `python3` or `python`. Standard library only.
- Network access to Maven Central. No Maven install needed; use the wrapper.
- Claude Code for the agent half, via the desktop app's Code tab or the CLI. On Windows it
  needs Git for Windows, which it uses to run hooks.
- GitHub Actions runs `./mvnw clean test` on JDK 11 for every push and pull request.

[SAMPLE_OUTPUT]
https://github.com/snbaem/aem-test-gate
