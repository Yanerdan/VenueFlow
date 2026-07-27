## Context

The Maven reactor, service integration profiles, Compose profiles, and runbooks already provide
individual checks. v0.8 needs a small, safe orchestration layer that verifies repository policy
without infrastructure and describes live fault actions without executing them by default.

## Goals / Non-Goals

**Goals:**

- Provide one cross-platform deterministic policy gate.
- Make required fault scenarios enumerable, bounded, reversible, and dry-run by default.
- Store evidence metadata without storing credentials or inventing outcomes.
- Reuse existing Maven, OpenSpec, Compose, and service recovery mechanisms.

**Non-Goals:**

- Running destructive chaos automatically, fabricating results, adding a chaos platform, load
  claims, real-user validation, or release/resume material.

## Decisions

1. PowerShell is the primary Windows fault driver and a portable shell policy gate covers CI.
   This matches the current repository without adding runtime dependencies.
2. Fault scenarios are versioned JSON descriptors. The driver only accepts known scenario IDs,
   defaults to plan mode, requires an explicit execution switch and environment file, and always
   prints the recovery command before mutation.
3. Live actions use scoped `docker compose stop/start` or bounded HTTP fixture instructions.
   They never delete volumes, alter schemas, or broaden targets.
4. Evidence is a sanitized JSON manifest containing scenario, UTC timestamps, declared target,
   command exit status, and operator note. Raw payloads, credentials, and invented metrics are
   excluded.
5. CI runs only the deterministic policy gate and existing verification. Live faults remain an
   operator action because shared CI infrastructure cannot prove recovery semantics safely.

## Risks / Trade-offs

- [A descriptor can drift from runtime names] -> deterministic checks validate all referenced
  Compose services and required recovery commands.
- [An operator enables the wrong target] -> exact allowlist, repository-root check, explicit
  execution flag, and no wildcard/container deletion commands.
- [Dry-run is mistaken for evidence] -> manifests distinguish `PLANNED` from `EXECUTED`; docs
  forbid treating plans as results.
- [Cross-platform scripts diverge] -> the shared policy contract is validated by local fixtures
  and CI; live mutation remains one PowerShell implementation.

## Migration Plan

Add scripts/descriptors and CI policy step first. Operators may generate plans immediately and
execute one scoped scenario only in an isolated local environment. Roll back by removing the CI
step and scripts; no business data or schema changes are involved.

## Open Questions

Measured fault and performance results remain intentionally absent until the user chooses to run
the isolated exercises.
