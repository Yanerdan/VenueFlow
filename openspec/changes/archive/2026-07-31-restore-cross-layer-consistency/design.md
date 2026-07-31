## Context

The business services enforce the correct invariants, but the synthetic seed writes directly across schemas and must reproduce the same capacity facts that normal APIs would create. Gateway rejection also terminates before downstream routing and therefore owns request-buffer cleanup.

## Decisions

- Keep completed/cancelled/expired historical showcase slots closed with zero occupancy.
- Keep pending/confirmed showcase slots open, insert idempotent `ALLOCATE` operations, and set occupancy to the reservation quantity.
- Remove only known local acceptance namespaces; never delete arbitrary user-created records.
- Drain and release request buffers before emitting 413.
- Add database assertions to the smoke script so future seed drift fails acceptance.

## Verification

- Gateway integration regression with repeated oversized bodies.
- Seed twice, assert no orphan showcase facts and no active capacity mismatch.
- Frontend tests, OpenSpec strict validation, Maven tests, and full-chain smoke.
