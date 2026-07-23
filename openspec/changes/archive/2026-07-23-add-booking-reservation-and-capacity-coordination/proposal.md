## Why

Booking Service now has a verified runtime boundary, while User Service owns booking
eligibility and Resource Service owns an idempotent capacity ledger. The next increment must
create the first durable reservation flow without sharing databases, bypassing service
ownership, or leaving ambiguous capacity writes unresolved.

The originally proposed design was syntactically complete but did not yet define atomic
Booking idempotency ownership, scoped idempotency keys, remote timeout reconciliation, or a
state model aligned with the engineering specification. An uncommitted partial implementation
also exists and must be audited against the corrected design rather than treated as completed
work.

## What Changes

- Add Booking-owned MySQL persistence for scoped idempotency records, reservations, and a
  bounded `CONFIRMED -> CANCELLED` lifecycle.
- Accept creation idempotency through the `Idempotency-Key` HTTP header and atomically obtain
  execution ownership before calling User or Resource.
- Add DTO-only create, retrieve, and cancel Booking APIs with the established success and safe
  error envelopes.
- Verify booking eligibility through User Service and allocate/release capacity through Resource
  Service using bounded Java HTTP adapters, deterministic operation IDs, and no automatic retry
  of writes.
- Add a Resource-owned lookup endpoint for one allocation operation ID so Booking can resolve
  an allocation timeout as success, rejection, or still unknown before deciding whether to
  persist or compensate.
- Make create replay, conflicting key reuse, cancellation replay, compensation, and optimistic
  state updates explicit and testable.
- Preserve Docker-free default verification and add opt-in isolated MySQL plus HTTP-stub
  integration evidence.
- Audit the existing uncommitted C10 implementation and rework only the parts that conflict
  with these artifacts.

## Capabilities

### New Capabilities

- `booking-reservation-management`: Booking-owned idempotency, reservation persistence,
  synchronous eligibility/capacity coordination, cancellation, and bounded compensation.

### Modified Capabilities

- `booking-service-skeleton`: Permit the explicit Booking persistence profile and bounded
  User/Resource HTTP adapters while preserving secret-free Docker-free skeleton startup.
- `slot-capacity-allocation`: Add a Resource-owned, operation-ID lookup required to resolve
  ambiguous allocation outcomes without exposing Resource persistence.

## Scope

This Change covers one synchronous v0.2 reservation slice:

```text
Idempotency-Key claim
  -> User eligibility read
  -> Resource capacity allocation
  -> Booking CONFIRMED persistence
  -> retrieval
  -> idempotent cancellation and Resource release
```

## Non-Scope

Authentication, authorization, Gateway, Feign, Nacos, Redis, RabbitMQ, Outbox, asynchronous
expiry, payment, check-in/completion, search, distributed transactions, shared tables,
cross-service database access, application-container orchestration, and production-grade
durable compensation recovery remain outside this Change.

## Impact

- `venueflow-booking-service`: persistence profile, Flyway V001, domain/application layers,
  HTTP adapters, APIs, safe errors, tests, README, and dependency enforcement.
- `venueflow-resource-service`: one DTO-only read endpoint and tests for allocation-operation
  lookup; no migration or ownership change.
- `.env.example`, root documentation, runbook, and HANDOFF: safe local configuration and
  verified execution instructions.

## Data Impact

- Additive Booking V001 only; it MUST create only Booking-owned reservation and idempotency
  facts.
- Resource schema remains unchanged because allocation operations already exist in V003.
- Existing Resource and User migrations MUST NOT be modified.

## Risks and Acceptance Boundary

- A process can still terminate after Resource allocation and before Booking persistence.
  Deterministic operation IDs, operation lookup, compensating release, logs, and a manual
  inspection procedure bound the risk for this synchronous increment. Durable automatic
  recovery is intentionally deferred to the Outbox/reconciliation milestone.
- A compensation release can itself fail. The request MUST return a stable
  `BOOKING_COMPENSATION_REQUIRED` failure with correlation data logged but no secret or SQL;
  operators use the documented inspection procedure until durable reconciliation is added.
- No retry is allowed for allocation or release writes. Only idempotent result lookup may use a
  small bounded retry budget.

## Acceptance Overview

- Concurrent identical creates produce one Booking and one effective Resource allocation.
- Reusing the same scoped key with a different request returns HTTP 409 before another Resource
  write.
- Allocation timeout is resolved through operation lookup; unresolved outcomes do not create a
  Booking and are reported distinctly.
- Local Booking persistence failure triggers one deterministic release attempt.
- Cancellation releases capacity before one optimistic local transition and is safe to replay.
- Default `clean verify` remains Docker-free; opt-in MySQL/HTTP-stub verification proves the
  migration, uniqueness, persistence, boundary failures, and compensation behavior.
