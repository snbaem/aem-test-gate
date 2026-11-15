---
name: aem-junit-agent
description: Generate a JUnit 5 unit test for a newly created AEM Java class (Sling Model, OSGi service, or Sling servlet), ask permission before saving, then run mvn test and repair failures. Use when a new class appears under src/main/java without a matching test, or when asked to "write a unit test" for an AEM class.
---

# AEM JUnit agent

Scope: Sling Models, OSGi services, Sling servlets. If the class is a workflow
process step, scheduler or event handler, say it is out of scope and stop.

## Flow (never skip the two permission gates)

1. **Read** the class. Note imports, injected fields, `@Reference`s, `@Activate`
   config, public methods, branches.
2. **Classify** using the table below. State the classification in one line.
3. **Draft** the test at the mirrored path: `src/main/java/x/Foo.java` ->
   `src/test/java/x/FooTest.java`. Cover happy path, null/blank/empty input,
   and each branch. Prefer behavior assertions over implementation details.
4. **Gate 1:** show the draft and ask: "Save this test to <path>?" Wait for yes.
5. Save, then **Gate 2:** ask "Run `mvn -q test -Dtest=FooTest`?" Wait for yes.
6. Run it. On failure, read the surefire output, fix the **test** (not the
   production code) and re-run, at most 2 repair loops. If still failing,
   report the failure and the likely cause, and stop. If the failure looks like
   a production bug, say so instead of bending the test.
7. Report: tests written, pass/fail, and JaCoCo line coverage for the class
   from `target/site/jacoco/jacoco.csv` when present.

## Classification and scaffolds

| Class looks like | Scaffold |
|---|---|
| `@Model` | `@ExtendWith(AemContextExtension.class)`, `AemContext context`, `context.addModelsForClasses(X.class)`, build content with `context.create().resource(path, props)`, adapt with `context.resourceResolver().getResource(path).adaptTo(X.class)` |
| `@Component(service=...)` with `@Activate`/config | `AemContext`, `context.registerInjectActivateService(new Impl(), Map.of(...))`, then `context.getService(Api.class)`. Test default and overridden config. |
| Extends `SlingSafeMethodsServlet`/`SlingAllMethodsServlet` | `AemContext`, register mock/Mockito service dependencies first (`context.registerService(Api.class, mock)`), `context.registerInjectActivateService(new Servlet())`, set params via `context.request().setParameterMap(...)`, call `servlet.doGet(context.request(), context.response())`, assert `context.response().getOutputAsString()` |

## Rules

- JUnit 5 only. Mockito via `mockito-junit-jupiter` for collaborators.
- Use `ResourceResolverType.RESOURCERESOLVER_MOCK` unless the class needs JCR
  queries or node types, in which case use `JCR_MOCK` and say why.
- Private fields with `@Reference`: register a collaborator in the context, do
  not reflect into the field.
- Do not edit production code unless the user asks.
- No real credentials, hostnames or customer content in test data. Use
  obviously fake values.
