## Why

Resource slots now describe when a resource can be used, but no durable fact records
how much of a slot has been reserved. A Resource-Service-owned allocation ledger is the
smallest safe next step before a Booking service can make capacity-changing requests.

## What Changes

- Add idempotent, operation-keyed capacity allocation and release for one open resource slot.
- Persist allocation facts and an auditable slot occupancy total with immutable Flyway migration(s).
- Provide internal Resource Service DTO APIs to allocate, release, and query slot capacity
  without adding a Booking aggregate, end-user identity, or external infrastructure.
- Extend Docker-free and opt-in MySQL verification to cover concurrent capacity safety and
  idempotency.

## Capabilities

### New Capabilities

- `slot-capacity-allocation`: Resource-owned idempotent allocation ledger and available
  capacity calculation for ResourceSlots.

### Modified Capabilities

- None.

## Impact

- Affected module: `venueflow-resource-service`, including Flyway migration, persistence,
  transactional application behavior, HTTP DTO endpoints, safe errors, and tests.
- Adds no Booking service, authentication, Redis, message broker, service discovery, or
  inter-service client; future Booking work will consume this capability.
