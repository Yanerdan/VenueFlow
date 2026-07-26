## Why

C10 can compensate a failed Booking finalization only while the request process remains alive.
A crash or ambiguous release can therefore leave Resource capacity allocated without a matching
Booking fact, which is the most important remaining gap after C11-C13 established reliable event
delivery.

## What Changes

- Persist bounded Booking-owned recovery intents around Resource allocation and release calls.
- Add leased, resumable reconciliation runs that compare Booking facts with Resource operation
  lookups through the existing DTO API; no cross-schema access is allowed.
- Automatically perform only the safe idempotent repair: release proven orphan capacity using
  the existing deterministic release operation ID.
- Record mismatches, attempts, outcomes, and operator reasons in bounded audit tables without
  collaborator bodies, credentials, or stack traces.
- Provide a non-HTTP preview/run command with dry-run default, explicit confirmation for repair,
  bounded batches, and safe metrics/logs.
- Keep default verification infrastructure-free and add an opt-in real MySQL plus HTTP-stub
  reconciliation suite.
- Exclude timeout expiration, new Booking states, Resource schema/API changes, Outbox repair,
  Notification repair, Gateway/security, and production scheduling/deployment.

## Capabilities

### New Capabilities

- `booking-capacity-reconciliation`: Durable recovery intents, bounded reconciliation runs,
  safe orphan-capacity repair, auditability, and verification.

### Modified Capabilities

- `booking-reservation-management`: Creation and cancellation coordination gain durable recovery
  intents so process termination no longer loses unresolved Resource outcomes.

## Impact

- **Booking Service:** new immutable V003 migration, reconciliation domain/application/persistence
  code, an internal command, configuration, metrics, tests, and runbook updates.
- **Resource Service:** no code or schema change; C14 uses its existing bounded operation lookup
  and idempotent release DTO contracts.
- **Runtime:** explicit `persistence,reconciliation` execution only; default `skeleton` and normal
  request handling create no scheduler or external connection beyond their existing profiles.
- **Operations:** new bounded environment settings and operator procedure for preview, confirmed
  repair, shutdown, and rollback.
