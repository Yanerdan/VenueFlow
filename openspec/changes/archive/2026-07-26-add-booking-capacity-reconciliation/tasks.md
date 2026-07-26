## 1. Boundaries, configuration, and schema

- [x] 1.1 Audit C10 reservation/compensation transactions, C11 Outbox atomicity, existing
  Resource operation DTOs, profiles, migrations, and integration fixtures before editing.
- [x] 1.2 Add an explicit `reconciliation` runtime profile and isolated `reconciliation-it`
  verification profile without adding a new production dependency.
- [x] 1.3 Add fail-fast, environment-driven settings for enablement, batch, lease, scan delay,
  attempts, backoff, operation lookup, and HTTP timeouts with safe bounded defaults.
- [x] 1.4 Add immutable Booking V003 for recovery intents, runs, issues, and repair actions with
  required uniqueness, state/version, lease/due-work, size, and retention indexes.

## 2. Durable request-path recovery intents

- [x] 2.1 Implement bounded intent domain types, stable outcomes/error codes, and persistence
  ports using explicit SQL for due-work claim and compare-and-set transitions.
- [x] 2.2 Persist one allocation intent atomically with new idempotency execution ownership before
  User/Resource calls, and resolve it in rejection/no-allocation paths.
- [x] 2.3 Resolve allocation intent atomically with reservation, idempotency success, and
  confirmation Outbox commit; preserve it when compensation is unproven or interrupted.
- [x] 2.4 Persist one cancellation intent before Resource release and resolve it atomically with
  the conditional cancellation transition and cancellation Outbox event.
- [x] 2.5 Prove request replays and losing concurrent transitions create no duplicate intent,
  Resource write, reservation transition, or Outbox event.

## 3. Leased reconciliation and safe repairs

- [x] 3.1 Implement bounded due-intent selection, version-CAS lease claim, lease expiry/reclaim,
  run accounting, next-check backoff, attempt exhaustion, and issue deduplication.
- [x] 3.2 Reconcile allocation intents through the existing Resource operation lookup, separating
  definitive absence, matching allocation, conflicting facts, and unknown outcomes.
- [x] 3.3 Release only a proven orphan allocation with the stored deterministic release operation;
  resolve ambiguous responses by lookup and record immutable repair attempts.
- [x] 3.4 Reconcile cancellation intents by proving or idempotently applying release, then commit
  `CONFIRMED -> CANCELLED`, one Outbox event, and intent resolution atomically.
- [x] 3.5 Handle process interruption, stale leases, concurrent workers, terminal Booking states,
  Resource unavailability, and persistence failure without blind writes or fact overwrites.

## 4. Execution controls and observability

- [x] 4.1 Add a scheduler that exists only under `persistence,reconciliation` and performs work
  only when explicitly enabled.
- [x] 4.2 Add a non-HTTP command with no-action default, metadata-only `PREVIEW`, and confirmed
  bounded `RUN` requiring a safe operator reason.
- [x] 4.3 Add payload-free metrics/logs for run, claim, consistent, repaired, unresolved, failed,
  lease-reclaimed, due depth, oldest age, action outcome, and shutdown.
- [x] 4.4 Implement bounded graceful shutdown so new claims stop and current lease/action
  transitions finish or remain safely reclaimable.

## 5. Deterministic and real-infrastructure verification

- [x] 5.1 Add unit tests for settings, intent transitions, operation classification, backoff,
  lease ownership/reclaim, repair policy, issue deduplication, and command confirmation.
- [x] 5.2 Add transaction and concurrency tests proving intent-before-call, atomic finalization,
  compensation preservation, cancellation/Outbox atomicity, crash windows, and replay safety.
- [x] 5.3 Preserve default skeleton/root verification and prove it creates no scheduler,
  datasource, collaborator connection, or Testcontainers fixture.
- [x] 5.4 Add `reconciliation-it` MySQL 8.4.10 evidence for V003, constraints, atomic intent
  transitions, competing worker claims, expired leases, runs, issues, and repair audit.
- [x] 5.5 Add bounded HTTP-stub evidence for orphan release, lost release response plus lookup,
  cancellation completion, conflicts, repeated outage/backoff, and idempotent recovery.

## 6. Documentation and final gates

- [x] 6.1 Update Booking/root README, `.env.example`, HANDOFF, and add a reconciliation runbook
  covering enablement, preview/run, metrics, issue handling, shutdown, migration, and rollback.
- [x] 6.2 Run module/root verification, `reconciliation-it`, Enforcer, Spotless, SpotBugs, SBOM,
  dependency, migration immutability, secret/path, service-boundary, and `git diff --check` gates.
- [x] 6.3 Run strict OpenSpec validation and confirm C14 changes no Resource code/schema/API,
  prior migration, production Compose, timeout state, Outbox publisher, Notification, or
  cross-service database access.
