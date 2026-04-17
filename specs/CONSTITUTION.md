# HL7-PET Constitution

## 1. Mission

HL7-PET is a Scala library for parsing, extracting, transforming, and validating HL7 v2 messages. Its purpose is to be a reliable, lightweight, framework-agnostic toolkit that other JVM projects can drop in without friction.

This document governs architectural decisions, API stability, and contribution standards for internal contributors.

---

## 2. Core Principles

**Balance performance with convenience.** Direct extraction and lazy evaluation are preferred, but not at the cost of an unusable API. If a convenience method can be built on top of a fast core, add it.

**Stay framework-agnostic.** The library must not depend on any application framework (Spring, Play, Akka, etc.). Dependencies should be minimal, widely available on the JVM, and justified explicitly before being added.

**HL7 v2 only.** The scope of this library is strictly HL7 v2. FHIR, HL7 v3, and other message formats are out of scope and should not be accommodated in the core design.

**Fail gracefully.** The parser returns empty/default results on malformed input rather than throwing. This is a deliberate design choice — do not break it.

**Profile-driven extensibility.** Message structure is defined in JSON profiles, not in code. New segment types or field definitions belong in profiles, not in hardcoded conditionals.

---

## 3. Architecture Boundaries

These constraints define what the library is. Violating them requires an explicit decision logged in `/specs`.

| Boundary | Rule |
|---|---|
| I/O | No I/O in core parsing or validation logic. File/stream utilities live in separate utility classes. |
| Frameworks | No framework dependencies in any non-optional module. |
| Reflection | No runtime reflection. |
| Message format | HL7 v2 pipe-delimited only. Delimiter must be `\|` with `^~\&` encoding characters. |
| External state | No global mutable state. All parsers and validators must be safe to use concurrently. |

---

## 4. API Stability

This library follows **Semantic Versioning** (`MAJOR.MINOR.PATCH`).

- **PATCH** — bug fixes with no API change.
- **MINOR** — new public API added; all existing API remains source- and binary-compatible.
- **MAJOR** — breaking changes allowed. Requires a clear migration note in the changelog.

### What counts as a breaking change

- Removing or renaming a public class, trait, method, or field.
- Changing a method signature (parameter types, return type, arity).
- Changing observable behavior that callers depend on (e.g., return value semantics, exception type thrown).
- Dropping support for a previously supported Scala or JVM version.

### What is not a breaking change

- Adding new public methods or classes.
- Fixing a bug where the old behavior was clearly incorrect.
- Internal refactoring with no change to the public API surface.
- Performance improvements.

### Scala cross-compilation

The library targets multiple Scala versions. Any change to the public API must compile and pass tests across all versions listed in `build.sbt` (`crossScalaVersions`). A PR that breaks a non-primary Scala version is not mergeable.

---

## 5. Dependency Policy

Before adding a new dependency, answer all three questions:

1. **Is it necessary?** Can the same result be achieved with a few lines of stdlib code?
2. **Is it framework-neutral?** It must not pull in a framework or require a specific runtime environment.
3. **Is it JVM-compatible?** It must work on all JVM versions in our support matrix.

If all three pass, add the dependency and document the rationale in the PR description. Transitive dependency bloat is a first-class concern — prefer libraries with minimal transitive footprints.

---

## 6. Testing Standards

- Every public API method must have at least one unit test covering its primary behavior.
- Edge cases (empty input, malformed HL7, missing segments) must be covered by dedicated tests.
- Tests must not rely on external services, file system state outside `src/test/resources`, or time-sensitive behavior.
- Tests must pass across all `crossScalaVersions`.

---

## 7. Contribution Workflow

Since this is currently a single-maintainer project, the workflow is lightweight:

- **Feature branches** off `master`. Name them descriptively (`feature/xxx`, `fix/xxx`).
- **PRs are required** for all non-trivial changes, even self-reviewed ones. This creates a record.
- **Changelog** — update `CHANGELOG.md` in every PR that changes behavior or API.
- **Breaking changes** — document the change and migration path in `/specs` before merging.

---

## 8. Amending This Constitution

Changes to this document require a PR with an explicit rationale. The `/specs` folder is the authoritative record of architectural decisions. When a decision is made that affects the constitution, add or update a spec file and reference it from here if needed.
