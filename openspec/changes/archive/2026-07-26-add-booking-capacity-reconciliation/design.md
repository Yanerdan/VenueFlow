## Context

Booking currently allocates Resource capacity before its local reservation transaction and
releases capacity before its local cancellation transition. Deterministic operation IDs and the
Resource operation lookup make ambiguous outcomes queryable, but unresolved work exists only in
the request process and logs. A process stop can therefore strand capacity or leave a confirmed
Booking after its release succeeded.

C11-C13 already provide reliable Booking events and idempotent Notification consumption. The
next smallest v0.5 reliability increment is durable Booking/Resource recovery; timeout expiration
and broader business-state expansion should remain separate Changes.

Constraints:

- Booking and Resource retain separate schemas; reconciliation uses DTO APIs only.
- Existing Resource allocate, release, and operation-lookup contracts remain unchanged.
- Resource writes are not blindly retried. Every ambiguous write is resolved by operation lookup.
- Default `skeleton` and normal `persistence` verification remain Docker-free.
- No scheduler may run unless an explicit reconciliation profile and enable flag are selected.

## Goals / Non-Goals

**Goals:**

- Persist recovery intent before every Resource allocation or release whose result affects a
  Booking transition.
- Recover safely after crashes at each cross-service transaction boundary.
- Release proven orphan allocations and complete proven cancellation transitions idempotently.
- Bound work, lease ownership, retries, timeouts, stored diagnostics, and operator actions.
- Preserve an auditable history of runs, issues, and repair attempts.
- Prove the crash windows with deterministic tests and opt-in real MySQL/HTTP evidence.

**Non-Goals:**

- Add `PENDING_CONFIRMATION`, `EXPIRED`, `COMPLETED`, check-in, or timeout cancellation.
- Scan Resource globally or change its schema/API.
- Repair Outbox, Notification, MySQL/ES, or historical data created before C14.
- Add Gateway, authentication, Nacos/Feign, Redis locks, distributed transactions, email, or
  production Compose scheduling.
- Delete or rewrite Booking V001/V002 or any other service migration.

## Decisions

### 1. Persist workflow intent instead of scanning both databases

Booking V003 creates:

- `booking_reconciliation_intent`: one durable `ALLOCATE` or `RELEASE` workflow with request,
  booking, slot, quantity, allocation/release operation IDs, state, lease, attempts, next check,
  stable outcome, and timestamps.
- `reconciliation_run`: bounded run ownership, trigger, counters, status, and timestamps.
- `reconciliation_issue`: deduplicated unresolved mismatch with severity, safe code, state, and
  first/last seen timestamps.
- `repair_action`: immutable attempted action, reason, result code, and timestamps.

An intent is written before the Resource call. Final Booking transactions resolve it atomically
with reservation/idempotency/Outbox or cancellation/Outbox facts. This captures only workflows
Booking owns and avoids a new Resource-wide enumeration API.

Alternative: periodically join schemas or scan all Resource allocations. Rejected because it
breaks service ownership or expands Resource contract and creates unbounded work.

### 2. Use the existing deterministic operations as proof

The reconciler leases a bounded page of due intents using status/version CAS. For each intent it
queries Resource by `slotId + operationId`:

- Allocation absent: resolve the create intent as `NO_ALLOCATION` when absence is definitive.
- Allocation present and matching Booking is `CONFIRMED`: resolve as already consistent.
- Allocation present with no successful Booking: invoke the deterministic release operation once;
  query the release operation if the response is ambiguous, then resolve only on proof.
- Release present for an open cancellation intent: conditionally transition
  `CONFIRMED -> CANCELLED`, append the cancellation Outbox event, and resolve in one transaction.
- Release absent for an open cancellation intent: make one idempotent release attempt for that
  run, resolve ambiguity by lookup, then perform the same local transition.
- Conflicting facts or repeatedly unknown outcomes: record/update an issue and schedule bounded
  backoff; never invent capacity or overwrite a terminal Booking state.

Alternative: repeat Resource writes whenever an HTTP call fails. Rejected because a timeout may
mean the write succeeded.

### 3. Keep repair policy narrow and deterministic

Automatic repair may only release a proven allocation for an unresolved create workflow or
finish a cancellation whose deterministic release is proven. It never reallocates capacity,
reopens a Booking, changes quantity, or repairs an unrelated operation. A repair action is
recorded before/after the attempt with stable codes and no collaborator payload.

This asymmetry favors avoiding capacity leaks without risking oversell or unauthorized business
state resurrection.

### 4. Use database leases, not Redis or an in-memory lock

Each intent has lease owner/expiry and optimistic version. A worker claims only due, nonterminal
rows in a short transaction; HTTP calls occur after commit. Expired leases can be reclaimed.
Run ownership is also persisted so a process stop is visible. Configuration bounds batch size,
lease duration, scan delay, attempts, backoff, and collaborator timeouts.

Alternative: `@Scheduled` plus a JVM lock. Rejected because it cannot recover ownership after a
crash or support multiple instances.

### 5. Separate automatic execution from the operator command

The `reconciliation` profile creates the runner. Automatic scanning additionally requires
`VENUEFLOW_RECONCILIATION_ENABLED=true`. A non-HTTP command supports:

- `PREVIEW`: list only bounded safe intent/issue metadata and make no repair.
- `RUN`: require a bounded reason and explicit confirmation, then process at most one configured
  batch using the same service as the scheduler.

No endpoint exposes repair over anonymous HTTP. Logs and metrics report run, claimed, consistent,
repaired, unresolved, failed, lease-reclaimed, and age/depth facts.

### 6. Preserve layered verification

Default tests use ports, repositories/fakes, and isolated application contexts for transition,
lease, crash-window, retry, command-guard, configuration, architecture, and logging behavior.
An explicit `reconciliation-it` profile uses MySQL 8.4.10 and bounded HTTP stubs to prove V003,
atomic intent transitions, competing workers, crash recovery, orphan release, cancellation
completion, and ambiguous-response lookup. It does not require RabbitMQ because Outbox insertion,
not publication, is the local invariant under test.

## Risks / Trade-offs

- [Work created before C14 has no intent] → Document that C14 guarantees new workflows only;
  historical repair requires a later controlled import/audit Change.
- [Resource is unavailable for a long period] → Retain the intent, apply bounded backoff, expose
  oldest age/depth, and never mark consistency without proof.
- [Repair succeeds but Booking crashes before recording it] → Deterministic release replay and
  operation lookup converge on the next lease.
- [Two workers claim the same row] → CAS lease/version permits one owner; all external writes are
  still idempotent as a second safety boundary.
- [Cancellation release succeeds while the Booking state changes concurrently] → Conditional
  transition and current-state reread either commit one cancellation event or raise a bounded
  issue without overwriting the winner.
- [Extra write on the request path] → Keep the intent row narrow and combine it with the existing
  idempotency/cancellation preparation transaction.

## Migration Plan

1. Deploy the new binary with reconciliation disabled; Flyway applies Booking V003.
2. Verify default and `persistence` request paths create/resolve intents correctly.
3. Run operator `PREVIEW` and inspect bounded depth/age metrics.
4. Enable one reconciler instance with a small batch and conservative backoff.
5. Increase concurrency only after issue and repair outcomes are stable.

Rollback disables reconciliation first, waits for leased work to finish, and rolls back the
binary. V003 remains in place and must not be edited or cleaned; the prior binary ignores the
additive tables. Open intents remain recoverable by a later compatible deployment.

## Open Questions

None. Timeout expiration and broader reconciliation domains are deliberately deferred.
