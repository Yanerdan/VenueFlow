## 1. Repository Baseline

- [x] 1.1 Add `.editorconfig`, `.gitattributes`, `.gitignore`, and `.env.example` with UTF-8/LF and secret-safe defaults.
  - Acceptance: Windows checkout keeps repository text normalized to LF; local `.env`, IDE files, logs, and build outputs are ignored.
  - Test: inspect `git check-attr` results and run a secret-pattern scan over tracked files.
- [x] 1.2 Add `.version/java.version`, `.version/maven.version`, and `.version/stack-versions.yml` using the approved baseline.
  - Acceptance: Java 21, Maven 3.9.16 and all frozen framework versions have one documented value.
  - Test: compare version files against effective Maven properties.

## 2. Maven Reactor

- [x] 2.1 Generate and commit Maven Wrapper 3.9.16 for Windows and Linux.
  - Acceptance: `mvnw.cmd -version` and `./mvnw -version` resolve Maven 3.9.16 without a global Maven requirement.
  - Test: run Wrapper version checks and verify wrapper metadata/download checksum configuration.
- [x] 2.2 Create the root aggregator/parent POM with JDK 21, UTF-8, reproducible build properties, modules, and fixed plugin versions.
  - Acceptance: the root POM contains no business dependencies and all plugin versions are explicit.
  - Test: run `mvnw.cmd help:effective-pom` and inspect dependency/plugin resolution.
- [x] 2.3 Create `venueflow-dependencies` and import the approved Spring Cloud, Spring Cloud Alibaba, and MyBatis-Plus BOMs.
  - Acceptance: downstream modules can consume managed dependencies without local core-version declarations.
  - Test: run Enforcer dependency convergence and inspect the effective BOM.

## 3. Minimal Common Module

- [x] 3.1 Create the `venueflow-common` aggregator and `venueflow-common-core` module without business-domain types or infrastructure clients.
  - Acceptance: the reactor compiles a real Java module while no service modules, Entities, Mappers, Redis, MQ, ES, or database dependencies exist.
  - Test: compile the reactor and run a structure/package-boundary assertion.
- [x] 3.2 Add at least one meaningful business-neutral JUnit test to prove test discovery and failure reporting.
  - Acceptance: the baseline build reports at least one executed test.
  - Test: run the module test and temporarily confirm a failing assertion fails the build before restoring it.

## 4. Quality Gates

- [x] 4.1 Configure Enforcer for Java/Maven versions, dependency convergence, duplicate classes, plugin versions, and release SNAPSHOT protection.
  - Acceptance: deliberate unsupported Java or dependency conflicts are rejected before packaging.
  - Test: run normal verification and targeted negative checks where practical.
- [x] 4.2 Configure Surefire, Failsafe, JaCoCo, Spotless, static analysis, CycloneDX, and Spring Boot Maven Plugin with fixed versions and scope-appropriate defaults.
  - Acceptance: default `clean verify` is deterministic and does not require Docker or unavailable business services.
  - Test: run formatting checks, unit tests, integration-test discovery, coverage aggregation, static checks, and SBOM generation.

## 5. Documentation and CI

- [x] 5.1 Add README, ADR template/initial ADR, and `.agent/HANDOFF.md` describing environment, modules, commands, non-goals, and the next Change.
  - Acceptance: a clean-checkout developer can identify JDK 21 requirements and run the build without relying on this conversation.
  - Test: follow the README commands from the repository root and check all referenced paths.
- [x] 5.2 Add a minimal GitHub Actions workflow using JDK 21 and Maven Wrapper for push and pull request verification.
  - Acceptance: CI performs only implemented checks and does not claim Docker, Migration, or business-test coverage.
  - Test: validate workflow syntax and ensure its build command matches the local acceptance command.

## 6. Final Verification

- [x] 6.1 Run `mvnw.cmd clean verify`, OpenSpec strict validation, repository hygiene checks, and review the complete Git diff.
  - Acceptance: all commands pass, no generated build output or secrets are staged, and every prior task has evidence.
  - Test: preserve command results in HANDOFF and mark tasks complete only after successful verification.
