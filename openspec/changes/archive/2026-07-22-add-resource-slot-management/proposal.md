## Why

The Resource Catalog now provides durable categories and physical resources, but it cannot express when a resource is available. A resource-owned time-slot capability is the next prerequisite for a later booking flow: it establishes the schedulable time facts without prematurely introducing reservations, capacity consumption, or cross-service coordination.

## What Changes

- Add a resource-owned slot model that records a single resource's start and end time in UTC and whether the slot is open for future use.
- Add management and bounded query APIs for creating, viewing, and opening or closing a resource's slots.
- Persist slots with an immutable Flyway migration and enforce core temporal and resource-existence invariants.
- Extend the existing test strategy with Docker-free service/web tests and opt-in MySQL integration coverage.

## Capabilities

### New Capabilities

- `resource-slot-management`: Maintain and query resource-owned, time-bounded slots that a later booking capability can consume.

### Modified Capabilities

- None.

## Impact

- Affected module: `venueflow-resource-service`, including a new Flyway migration, persistence model, application service, REST APIs, error mapping, and tests.
- Adds no runtime infrastructure or external service dependencies; the existing optional MySQL/Testcontainers integration-test profile remains the persistence verification path.
- Adds no booking, inventory allocation, authentication, notification, Redis, message broker, or inter-service client behavior.
