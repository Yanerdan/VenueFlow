## Context

Resource Service owns static Resource capacity and non-overlapping ResourceSlots, but no
mutable occupancy fact. Booking must eventually request capacity safely, while the current
repository has no Booking service or cross-service transaction mechanism.

## Goals / Non-Goals

**Goals:**

- Maintain a durable, idempotent operation ledger for allocation and release against one
  `OPEN` slot.
- Never let confirmed allocation exceed the parent Resource's static capacity, including
  concurrent requests.
- Expose only resource-owned internal-style DTO APIs and auditable capacity queries.

**Non-Goals:**

- Booking aggregate/lifecycle, user identity, payment, cancellation policy, check-in,
  approval, expiry jobs, notifications, recurring slots, or cross-service clients.
- Redis locks, messages, distributed transactions, or a derived capacity cache.

## Decisions

### Use an append-only operation ledger plus a materialized slot total

V003 will add `resource_slot_allocation` keyed by a caller-supplied operation id and a
per-slot `allocated_quantity` column. Each successful allocation/release records a
positive quantity and operation type. Replaying the same operation id with identical
request facts returns the original result; a conflicting replay is rejected. This provides
auditability and an integration-safe idempotency boundary without a Booking table.

### Serialize capacity changes by locking the slot row

The application locks the slot row in a short transaction, verifies it is OPEN, loads its
parent Resource capacity, checks the operation ledger, and conditionally updates the
occupied total. This avoids read-modify-write oversubscription using standard MySQL row
locks; a distributed lock was rejected because Resource Service is already the single
owner of these facts.

### Treat release as an operation, not deletion

Release references a distinct operation id and lowers the occupied total only when it
does not become negative. The ledger is retained. Mapping a release to a future Booking
identifier is deferred, so this increment intentionally cannot decide business
cancellation eligibility.

## Risks / Trade-offs

- [Slot row locking serializes writes for a busy slot] → Capacity writes are bounded and
  correctness-first; revisit only with measured contention.
- [No Booking reference in the ledger] → The operation id is the stable correlation point;
  Booking will define its own mapping in a later change.
- [Static Resource capacity can later change] → Resource capacity is immutable in current
  scope; any future edit policy must specify allocation compatibility.

## Migration Plan

1. Apply additive V003 after V001/V002; no existing slot is allocated initially.
2. Deploy Resource Service with existing persistence settings and use allocation APIs only
   from trusted development callers until Booking exists.
3. Never rewrite V003; follow-up ledger corrections use compensating operations or a new
   migration.

## Open Questions

- A later Booking proposal will define caller authentication, booking-to-operation mapping,
  release/cancellation rules, and time-based expiry.
