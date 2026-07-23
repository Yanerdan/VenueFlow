## 1. Baseline and configuration boundaries

- [x] 1.1 Audit C10 Booking transaction, profile, dependency, and test boundaries before editing.
  - Acceptance: the implementation plan identifies the exact confirmation/cancellation
    transaction entry points and preserves all C10 idempotency, compensation, and migration
    behavior.
  - Test: scoped source/diff review and unchanged C10 default/MySQL suites.
- [x] 1.2 Add only approved Spring AMQP and RabbitMQ Testcontainers dependencies with enforcement.
  - Acceptance: forbidden service, discovery, stream, security, Redis, and tracing dependencies
    remain rejected; versions stay centrally managed.
  - Test: Maven dependency tree, Enforcer, convergence, and SBOM checks.
- [x] 1.3 Add an explicit `messaging` profile and validated bounded publisher configuration.
  - Acceptance: `skeleton` and persistence-only startup make no RabbitMQ connection; messaging
    requires persistence and environment-only credentials; lease exceeds confirm timeout.
  - Test: configuration binding/validation and profile boundary context tests.

## 2. Outbox schema and atomic event creation

- [x] 2.1 Add immutable Booking `V002__add_booking_outbox.sql`.
  - Acceptance: only `booking_outbox_event` is added with unique event/business identity,
    lifecycle/type checks, immutable bounded content, retry/lease facts, versions, and timestamps;
    V001 and other services' migrations are unchanged.
  - Test: static migration review and fresh MySQL V001→V002 integration test.
- [x] 2.2 Implement typed Outbox domain models and deterministic bounded event serialization.
  - Acceptance: confirmed/cancelled envelopes and routing keys are versioned, UTC, stable,
    size-checked, UTF-8 JSON, and secret-free.
  - Test: exact JSON/routing fixtures, size rejection, malformed input, and leakage tests.
- [x] 2.3 Append confirmation Outbox events in the existing Booking finalization transaction.
  - Acceptance: reservation, confirmation event, and idempotency success commit together;
    insertion failure rolls all local facts back and retains deterministic C10 compensation.
  - Test: transaction commit/rollback, create replay, conflict, and compensation tests.
- [x] 2.4 Append cancellation Outbox events only when the optimistic transition wins.
  - Acceptance: one `CONFIRMED -> CANCELLED` mutation creates one event; replay/losing concurrent
    updates create none and retain C10 Resource release semantics.
  - Test: cancellation success, replay, concurrent update, stale version, and rollback tests.

## 3. Multi-instance-safe Outbox persistence

- [x] 3.1 Implement bounded eligible-row claim using explicit MySQL locking SQL.
  - Acceptance: a short transaction claims `NEW`, due `RETRY`, and expired `PUBLISHING` rows with
    `FOR UPDATE SKIP LOCKED`, unique token, lease, deterministic order, and bounded batch.
  - Test: repository SQL tests and two-instance real-MySQL concurrent claim test.
- [x] 3.2 Implement claim-token-guarded publish success and failure finalization.
  - Acceptance: only the current token can mark `PUBLISHED`, `RETRY`, or `DEAD`; stale workers
    cannot overwrite reclaimed rows; immutable event content never changes.
  - Test: transition matrix, stale token, expired lease, optimistic conflict, and audit tests.
- [x] 3.3 Implement safe metadata inspection and explicit dead-event requeue application command.
  - Acceptance: preview and confirmed requeue require event ID/operator reason, preserve event
    identity/content/audit, reject published/active events, and expose no anonymous mutation API.
  - Test: command validation, allowed/forbidden state, content immutability, and leakage tests.

## 4. Confirmed RabbitMQ publication

- [x] 4.1 Configure the producer-owned durable topic exchange and persistent mandatory messages.
  - Acceptance: Booking declares no consumer queue; event ID correlation, content type/encoding,
    delivery mode, routing keys, connection recovery, and timeouts follow the design.
  - Test: messaging configuration tests and topology inspection in RabbitMQ integration.
- [x] 4.2 Implement a typed RabbitMQ adapter that combines Publisher Confirm and Publisher Return.
  - Acceptance: only ACK plus routed evidence succeeds; NACK, return, timeout, connection failure,
    malformed message, and interruption map to stable outcomes; interrupt status is preserved.
  - Test: deterministic adapter outcome matrix and real broker routed/unroutable tests.
- [x] 4.3 Implement bounded scanner orchestration outside database transactions.
  - Acceptance: scanner claims, commits, publishes, then finalizes; batch/worker count is bounded;
    no broker wait, retry delay, or network call occurs in a DB transaction.
  - Test: orchestration order, transaction-boundary, partial-batch, shutdown, and disabled-profile
    tests.

## 5. Retry, recovery, and operational evidence

- [x] 5.1 Implement deterministic capped exponential retry and terminal `DEAD` behavior.
  - Acceptance: each failed owned claim increments once, schedules a bounded future attempt, and
    stops automatic claiming at the configured maximum.
  - Test: clock-controlled backoff boundaries, retry exhaustion, and no infinite retry tests.
- [x] 5.2 Add safe internal metrics and structured publisher outcome logs.
  - Acceptance: backlog/oldest age, claims, confirms, returns, retries, dead events, event ID,
    claim token, and stable outcome are observable without payloads, credentials, URLs, or raw
    exceptions; restricted Actuator exposure is unchanged.
  - Test: meter/log assertions, sensitive-data scan, and management endpoint tests.
- [x] 5.3 Prove lease and post-confirm crash recovery without claiming exactly-once delivery.
  - Acceptance: expired claims recover; stopping after broker ACK but before MySQL finalization
    permits the same event ID to publish again while losing no event.
  - Test: controlled crash-window integration scenario and duplicate-event-ID assertion.

## 6. Verification and handoff

- [x] 6.1 Add Docker-free unit, architecture, and configuration verification.
  - Acceptance: default tests cover envelope, transactions via ports, state machine, claiming,
    retry, adapter outcomes, profile isolation, dependency boundaries, and executable skeleton
    without starting Docker.
  - Test: Booking module and root default `clean verify`.
- [x] 6.2 Add opt-in combined MySQL 8.4 and RabbitMQ 4.1.8 `outbox-it` suite.
  - Acceptance: V002, atomicity, concurrent claim, lease recovery, persistent routed ACK,
    mandatory return, retry/dead, and duplicate-attempt recovery pass against fresh containers;
    default verify never activates the suite.
  - Test: documented `outbox-it` Maven command and Failsafe reports.
- [x] 6.3 Update `.env.example`, Booking/root README, Outbox runbook, and `.agent/HANDOFF.md`.
  - Acceptance: documents cover profiles, variables, topology ownership, enable/disable order,
    inspection, replay, backlog/unroutable/confirm-timeout response, at-least-once limitation, and
    exact verification commands without secrets or personal paths.
  - Test: documentation review and tracked secret/absolute-path scan.
- [x] 6.4 Run final validation and scope review.
  - Acceptance: Booking/root verify, `mysql-it`, `outbox-it`, strict OpenSpec validation,
    `git diff --check`, migration immutability, dependency/service-boundary review, and task
    evidence all pass; no consumer, timeout job, Resource Outbox, or reconciliation code appears.
  - Test: exact commands and scoped diff inventory recorded in HANDOFF.
