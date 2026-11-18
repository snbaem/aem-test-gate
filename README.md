# AEM Test Gate

*(formerly "AEM JUnit agent")*

Generic sample AEM-style project (no proprietary code) used to demo a Claude Code
skill + hook that writes unit tests for new Sling Models, OSGi services and servlets.

- `.claude/hooks/detect_new_java_class.py` : PostToolUse hook; fires when a new class under
  `src/main/java` has no mirrored `*Test.java`.
- `.claude/skills/aem-test-gate/SKILL.md` : classify, draft, ask to save, ask to run, repair (max 2 loops).
- `scripts/test_gap_audit.py` : ranked "which classes have no test" report; no agent needed.
- `scripts/coverage_gate.py` : JaCoCo line-coverage gate; no agent needed.
- `src/test/java/...` : reference tests (the shape the agent should produce).
- `src/main/java/.../models/TeaserModel.java` + `TeaserModelTest.java` : a worked example, produced
  by one real run of the agent (14 tests, 100% line and branch coverage), kept as a reference.

## Why this, and not just asking Claude for a test?

Claude Code, in the terminal or the VS Code extension, can already write a JUnit test when
asked. This repo doesn't add that ability. It turns it into a **repeatable, team-wide workflow**:
automatic, consistent and gated, and versioned with the code. The VS Code extension loads this
project's `.claude/` folder too, so the comparison is ad-hoc prompting versus a codified recipe.

| | Ad-hoc ("write a JUnit for this class") | This recipe |
|---|---|---|
| **When it happens** | Only if the developer remembers to ask | The hook notices a new class with no test right after Claude writes it, and starts the workflow |
| **How the test is written** | Differs by developer, prompt and run | One approach: classify (Sling Model / OSGi service / servlet), use the matching AEM Mocks scaffold, cover the happy path, null and blank input, and each branch |
| **AEM know-how** | Up to the model each time (Mockito everything, reflect into private fields, wrong resource resolver) | Written down: `AemContext`, `RESOURCERESOLVER_MOCK` unless JCR is needed, register collaborators instead of reflecting, fake data only |
| **Safety** | May save files, run builds, or change production code to make a test pass | Two permission gates (save, run); fixes the test and not the class; at most 2 repair attempts; flags likely production bugs instead of bending the test |
| **Proof it works** | You get a file | Runs the test, repairs it, and reports pass/fail plus JaCoCo coverage |
| **Sharing** | Lives in one person's chat history | Committed to git: reviewed in PRs and picked up by everyone who clones the repo |

### The recipe

1. **Trigger (hook):** `.claude/hooks/detect_new_java_class.py` decides *when* to act.
2. **Procedure (skill):** `.claude/skills/aem-test-gate/SKILL.md` decides *how*: classify, draft,
   gate, run, repair, report.
3. **Exemplars:** the reference tests in `src/test/java` show *what good looks like*.
4. **A build where tests can pass:** AEM SDK API on the test classpath, `bnd-maven-plugin` for OSGi
   component metadata (without it, OSGi Mock fails with `NoScrMetadata`), and a pinned Maven Wrapper.
5. **Proof (CI):** GitHub Actions runs the tests on every push and pull request.

Steps 1–3 can be copied into any AEM project. Step 4 is usually already in place in a project
generated from the AEM Project Archetype.

### Limits

- The hook fires only when **Claude** writes a file (`Write` tool). For a class a developer creates
  by hand in the IDE, run `python3 scripts/test_gap_audit.py` to find it, or ask Claude to use the
  skill. Wiring that script into pre-commit or CI would close the gap entirely.
- Scope is Sling Models, OSGi services and servlets. Workflow steps, schedulers and event handlers
  are out of scope.
- Demonstrated on sample classes, not a large production codebase.

## How this compares to the published AEM recipes

The Experience League [AI Recipe Library](https://experienceleaguecommunities.adobe.com/ai-recipe-library-224)
already covers the two halves of this, both by Narendra Gandhi. This repo is deliberately
built on top of them rather than repeating them.

- **AEM Junit Skill** — generates AEM test templates on demand
  (`npx aem-junit-skill template model`), installable in Codex, Claude or Copilot. If
  templates are all you need, use that: it works outside Claude Code, which this does not.
- **Use VS Code GitHub Copilot Hooks to Run Semgrep in an AEM Project** — establishes the
  PostToolUse hook pattern: the agent edits a file, a hook fires, a repo-owned runner
  script checks the source.

Measured against those two as published, what is left here:

| Capability | Junit Skill | Semgrep hook | This repo |
| --- | --- | --- | --- |
| Fires without being asked | no | yes | yes |
| Detects that a test is *missing* | — | no | **yes** |
| Classifies the class before choosing a scaffold | yes | — | yes |
| Writes the test | yes | — | yes |
| Executes it | no | — | **yes** |
| Repairs its own failures | no | no | **yes, max 2** |
| Reports whether coverage moved | no | no | **yes** |
| Human gate before save and before run | — | — | **yes** |

The contribution is the **verify-and-repair loop behind human gates**, plus the gap
detection that decides when the loop should start. Not test generation, and not hooks.

## Audit a codebase without the agent

Both scripts are plain Python 3 with no dependencies, and work on any AEM Maven project:

```bash
# Which classes have no test at all, ranked by branch count
python3 scripts/test_gap_audit.py
python3 scripts/test_gap_audit.py --json          # machine-readable
python3 scripts/test_gap_audit.py --fail-on-gap   # exit 1 if any class lacks a test

# Line coverage, overall or for one class
./mvnw clean test
python3 scripts/coverage_gate.py --min 60
python3 scripts/coverage_gate.py --class HeroModel
```

`test_gap_audit.py` skips interfaces, enums and annotations, classifies each class, and
names the scaffold the skill would use. It is the part worth copying even if you never
run the agent half.

## Roadmap

The `.claude/` folder separates **when** to act (hook) from **how** to act (skill). New triggers
can reuse the skill, new skills can plug into the same hook pattern, and all of it ships with the
repo.

**More skills**
- Cover what's out of scope today: workflow process steps, schedulers, event handlers.
- Coverage-gap skill: "bring package X to N% line coverage". `scripts/coverage_gate.py` now reads
  the JaCoCo CSV the build produces; the skill that acts on it is still to write.
- Test-review skill: check existing tests against the same conventions.
- Migration skills: JUnit 4 → 5, PowerMock → AEM Mocks.

**More hooks**
- Match `Edit` as well as `Write`, so new public methods on existing classes prompt test updates.
- `SessionStart`: check the JDK and warn when Maven would run on Java 8.
- `Stop`: run `./mvnw test` before Claude finishes, so a session can't end with a failing build.
- `PreToolUse`: block edits under `src/main` while the skill runs, so "fix the test, not the
  class" is enforced rather than only instructed.

**More of `.claude/`**
- `CLAUDE.md`: always-on project conventions beyond tests.
- `agents/`: a test-writer subagent limited to `src/test`, and a reviewer subagent.
- `permissions` in `settings.json`: pre-approve `./mvnw test`, deny risky commands.

**Wider rollout**
- Package `.claude/` as a Claude Code **plugin** an organisation installs once, instead of copying
  the folder into each repo.
- Run Claude Code **headless in CI** (`claude -p` or the Claude Code GitHub Action) on pull
  requests to find new classes without tests and suggest them. This closes the gap for classes
  developers create by hand.

## Is it Claude-only?

| Part | Portability |
|---|---|
| **Runtime** (hook firing, skill loading, the gates) | Claude Code, which runs Claude models (directly from Anthropic or through cloud platforms such as Amazon Bedrock or Google Vertex AI). |
| **Skill** (`SKILL.md`) | Plain Markdown with a short header. Other coding agents that read the Agent Skills format may load it as-is (check each tool's docs). Otherwise its content can go into another tool's instructions file (e.g. `AGENTS.md`) unchanged. |
| **Hook script** | Generic Python, JSON in and JSON out. The wiring in `.claude/settings.json` is Claude Code-specific; other tools would need a small adapter. |
| **Build, reference tests, CI** | No AI dependency. Useful with any model, or none. |

The conventions, the gated workflow and a build where AEM tests pass are model-agnostic. The
automation that ties them together is a Claude Code feature, and that's the runtime this repo was
tested on.

## Try it

### Prerequisites

- **JDK 11 or newer.** Check with `java -version`. If your default is Java 8, point `JAVA_HOME`
  at a JDK 11+ for the commands below (Java 8 fails with `invalid flag: --release`).
- **Python 3** on `PATH` as `python3` or `python` (the hook is pure stdlib).
- **Network access to Maven Central.** No Maven install needed: use the wrapper (`./mvnw`, or
  `mvnw.cmd` on Windows), which pins Maven 3.9.9, the same version CI uses.
- **[Claude Code](https://docs.claude.com/en/docs/claude-code/overview).** On Windows it needs
  Git for Windows (Git Bash), which it uses to run hooks.

Get the code:

```bash
git clone https://github.com/snbaem/aem-test-gate.git
cd aem-test-gate
```

### 1. Check that the project builds

```bash
./mvnw clean test
```

Expected: `Tests run: 25, Failures: 0, Errors: 0` and `BUILD SUCCESS`
(`HeroModelTest` 4, `TeaserModelTest` 14, `GreetingServiceImplTest` 4, `GreetingServletTest` 3).

On Windows PowerShell, run `.\mvnw.cmd clean test`. To use JDK 11 for one session:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.x.y'   # your JDK 11+ folder
.\mvnw.cmd clean test
```

### 2. Check the hook on its own (optional, no Claude needed)

From the repo root, send the hook the same event Claude Code sends after a write
(use `python3` instead of `python` on macOS/Linux). On Windows, run this in **Git Bash**, not
PowerShell: Windows PowerShell adds a byte-order mark when piping text, the hook can't parse the
JSON, and it prints nothing.

```bash
echo '{"tool_name":"Write","tool_input":{"file_path":"src/main/java/com/example/demo/models/NoTestYet.java"}}' | python .claude/hooks/detect_new_java_class.py
```

Expected: one line of JSON whose `additionalContext` starts with
`A new AEM Java class was created at ...`. With `HeroModel.java` or `TeaserModel.java`, which
already have tests, it should print nothing.

### 3. Run the agent end to end

1. Open this folder in Claude Code. Start a **new** session so it loads the project's
   hook and skill.
2. Ask for a class that doesn't exist yet. Don't reuse `TeaserModel`: it already has a test, so
   the hook correctly stays silent. For example:
   > Create a Sling Model `PromoTeaserModel` in `com.example.demo.models` with optional `title`,
   > `description` and `linkUrl` properties. Trim values, add `.html` to internal `/content`
   > links, and add an `isConfigured()` that needs both a title and a link.

   This is the same spec that produced `TeaserModel`, so you can compare your run with it.
   Any new class name works.
3. Confirm each of these happens:

| # | What you should see | If it doesn't happen |
|---|---|---|
| 1 | Claude writes `src/main/java/.../PromoTeaserModel.java` | — |
| 2 | Right after the write, Claude says the class has no test and starts the **aem-test-gate** skill (the hook fired) | Run step 2 above. Check `python3`/`python` is on `PATH` and you started a new session. |
| 3 | It classifies the class in one line (here: Sling Model, `@Model`) | — |
| 4 | It shows a draft `PromoTeaserModelTest` covering the happy path, null and blank values, and each branch | — |
| 5 | **Gate 1:** it asks `Save this test to src/test/java/.../PromoTeaserModelTest.java?` and waits | It must not save before you say yes. |
| 6 | **Gate 2:** after saving, it asks to run `./mvnw -q test -Dtest=PromoTeaserModelTest` and waits | It must not run Maven before you say yes. |
| 7 | Tests pass. If they don't, it fixes the **test** (not the class), at most 2 times | `invalid flag: --release` means Maven is on Java 8; see Prerequisites. |
| 8 | It reports the result and JaCoCo line coverage for the class | — |

Saying **No** at either gate should stop the flow there.

Clean up afterwards with `git clean -n src/` (preview), then `git clean -f src/`, or delete the
two generated files (`PromoTeaserModel.java` and `PromoTeaserModelTest.java`). This removes only
untracked files, so the committed `TeaserModel` example stays.

Every push and pull request also runs `./mvnw clean test` on JDK 11 in GitHub Actions
(`.github/workflows/test.yml`).
