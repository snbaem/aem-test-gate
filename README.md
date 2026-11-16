# AEM JUnit agent (demo project)

Generic sample AEM-style project (no proprietary code) used to demo a Claude Code
skill + hook that writes unit tests for new Sling Models, OSGi services and servlets.

- `.claude/hooks/detect_new_java_class.py` : PostToolUse hook; fires when a new class under
  `src/main/java` has no mirrored `*Test.java`.
- `.claude/skills/aem-junit-agent/SKILL.md` : classify, draft, ask to save, ask to run, repair (max 2 loops).
- `src/test/java/...` : reference tests (the shape the agent should produce).

## Try it

### Prerequisites

- **JDK 11 or newer.** Check with `java -version`. If your default is Java 8, point `JAVA_HOME`
  at a JDK 11+ for the commands below (Java 8 fails with `invalid flag: --release`).
- **Python 3** on `PATH` as `python3` or `python` (the hook is pure stdlib).
- **Network access to Maven Central.** No Maven install needed: use the wrapper (`./mvnw`, or
  `mvnw.cmd` on Windows), which pins Maven 3.9.9, the same version CI uses.
- **Claude Code.** On Windows it needs Git for Windows (Git Bash), which it uses to run hooks.

### 1. Check that the project builds

```bash
./mvnw clean test
```

Expected: `Tests run: 11, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

On Windows PowerShell, run `.\mvnw.cmd clean test`. To use JDK 11 for one session:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.x.y'   # your JDK 11+ folder
.\mvnw.cmd clean test
```

### 2. Check the hook on its own (optional, no Claude needed)

From the repo root, send the hook the same event Claude Code sends after a write
(use `python3` instead of `python` on macOS/Linux):

```bash
echo '{"tool_name":"Write","tool_input":{"file_path":"src/main/java/com/example/demo/models/NoTestYet.java"}}' | python .claude/hooks/detect_new_java_class.py
```

Expected: one line of JSON whose `additionalContext` starts with
`A new AEM Java class was created at ...`. With `HeroModel.java`, which already has a test,
it should print nothing.

### 3. Run the agent end to end

1. Open this folder in Claude Code. Start a **new** session so it loads the project's
   hook and skill.
2. Ask, for example:
   > Create a Sling Model `TeaserModel` in `com.example.demo.models` with optional `title`,
   > `description` and `linkUrl` properties. Trim values, add `.html` to internal `/content`
   > links, and add an `isConfigured()` that needs both a title and a link.
3. Confirm each of these happens:

| # | What you should see | If it doesn't happen |
|---|---|---|
| 1 | Claude writes `src/main/java/.../TeaserModel.java` | — |
| 2 | Right after the write, Claude says the class has no test and starts the **aem-junit-agent** skill (the hook fired) | Run step 2 above. Check `python3`/`python` is on `PATH` and you started a new session. |
| 3 | It classifies the class in one line (here: Sling Model, `@Model`) | — |
| 4 | It shows a draft `TeaserModelTest` covering the happy path, null and blank values, and each branch | — |
| 5 | **Gate 1:** it asks `Save this test to src/test/java/.../TeaserModelTest.java?` and waits | It must not save before you say yes. |
| 6 | **Gate 2:** after saving, it asks to run `./mvnw -q test -Dtest=TeaserModelTest` and waits | It must not run Maven before you say yes. |
| 7 | Tests pass. If they don't, it fixes the **test** (not the class), at most 2 times | `invalid flag: --release` means Maven is on Java 8; see Prerequisites. |
| 8 | It reports the result and JaCoCo line coverage for the class | — |

Saying **No** at either gate should stop the flow there.

Clean up afterwards with `git clean -n src/` (preview), then `git clean -f src/`, or delete the
two generated files.

Every push and pull request also runs `./mvnw clean test` on JDK 11 in GitHub Actions
(`.github/workflows/test.yml`).
