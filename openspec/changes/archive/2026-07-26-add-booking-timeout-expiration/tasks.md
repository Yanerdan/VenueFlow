## 1. Boundaries configuration and schema

- [x] 1.1 Audit C10/C11/C13/C14 reservation, Outbox, Notification, reconciliation, profiles,
  migrations, and integration fixtures before editing.
- [x] 1.2 Add an explicit `expiration` runtime profile and isolated `expiration-it` Maven profile
  without adding a production dependency or changing default startup.
- [x] 1.3 Add fail-fast bounded settings for confirmation window, enablement, batch, lease, scan
  delay, attempts, backoff, Resource lookup, and HTTP timeouts.
- [x] 1.4 Add immutable Booking V004 for pending/expired lifecycle, deadline, timeout lease/retry,
  reason, due-work indexes, and status audit while preserving existing rows.

## 2. Pending creation confirmation and cancellation

- [x] 2.1 Extend bounded Booking domain/DTO/error types with pending, expired, deadline, and stable
  deadline/timeout conflict outcomes.
- [x] 2.2 Change creation finalization to atomically persist `PENDING_CONFIRMATION`, idempotency
  success, deadline, status audit, and allocation-intent resolution without a confirmation event.
- [x] 2.3 Add DTO-only idempotent confirmation that conditionally commits a non-expired pending
  reservation, one confirmation Outbox event, and one status audit in one transaction.
- [x] 2.4 Extend cancellation to pending reservations, reject live timeout ownership, preserve
  deterministic release/recovery, and keep expired reservations terminal.
- [x] 2.5 Update C14 allocation reconciliation so matching pending reservations are consistent and
  never treated as orphan capacity.

## 3. Leased timeout expiration

- [x] 3.1 Implement bounded due selection, version-CAS timeout claim, unique lease owner,
  lease expiry/reclaim, retry backoff, exhaustion, and safe issue facts.
- [x] 3.2 Resolve the existing deterministic Resource release operation before any write and make
  at most one idempotent release call per claimed run when absence is definitive.
- [x] 3.3 Resolve ambiguous release by lookup and atomically commit `EXPIRED`, status audit,
  expiration Outbox event, and lease completion only after release proof.
- [x] 3.4 Enforce one winner across confirmation, pending cancellation, and expiration without
  duplicate Resource effects, state logs, or Outbox events.
- [x] 3.5 Handle interruption, stale lease, terminal state, mismatch, Resource outage, and
  persistence failure without blind writes or late confirmation.

## 4. Events notification controls and observability

- [x] 4.1 Add the bounded `booking.reservation.expired.v1` Outbox allowlist/envelope and keep
  confirmation publication in the confirmation transaction only.
- [x] 4.2 Add Notification validation, exact binding, typed content, and inbox-idempotent handling
  for expiration through existing manual ACK/retry/DLQ/replay paths.
- [x] 4.3 Add an expiration scheduler only under `persistence,expiration`, disabled unless the
  explicit enable flag is true.
- [x] 4.4 Add a non-HTTP no-action default command with metadata-only `PREVIEW` and bounded
  confirmed one-batch `RUN`.
- [x] 4.5 Add payload-free deadline, due age/depth, claim, race winner, expired, retry, mismatch,
  lease-reclaimed, release outcome, event, notification, and shutdown metrics/logs.

## 5. Deterministic and real-infrastructure verification

- [x] 5.1 Add unit tests for lifecycle/deadline validation, configuration, CAS transitions,
  backoff/exhaustion, Resource classification, command guards, and event derivation.
- [x] 5.2 Add transaction/concurrency tests for pending creation replay, confirmation event
  atomicity, pending cancellation, deadline rejection, and three-way race safety.
- [x] 5.3 Preserve default skeleton/root verification and prove it creates no expiration scheduler,
  datasource, collaborator, broker, or Testcontainers fixture.
- [x] 5.4 Add `expiration-it` MySQL 8.4.10 and HTTP-stub evidence for V004, deadlines, competing
  claims, expired leases, lost release response, retry, and one expiration event.
- [x] 5.5 Extend `outbox-it` and `consumer-it` RabbitMQ 4.1.8 evidence for routed persistent
  expiration publication, manual ACK, duplicate delivery, retry, and dead-letter behavior.

## 6. Documentation migration and final gates

- [x] 6.1 Update Booking/Notification/root README, `.env.example`, HANDOFF, API compatibility
  notes, and add an expiration runbook covering rollout, preview/run, issues, shutdown, rollback.
- [x] 6.2 Run module/root verification, `mysql-it`, `expiration-it`, `outbox-it`, `consumer-it`,
  Enforcer, Spotless, SpotBugs, SBOM, dependency, migration, secret/path, and boundary gates.
- [x] 6.3 Run strict OpenSpec validation and confirm C15 changes no Resource code/schema/API,
  prior migration, production Compose, payment, Redis, Gateway/security, or cross-service DB access.
