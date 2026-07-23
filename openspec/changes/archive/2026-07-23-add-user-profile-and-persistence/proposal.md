## Why

VenueFlow can now allocate ResourceSlot capacity and run an independent User Service, but
it has no User Service-owned identity profile or eligibility fact for a future Booking flow
to consult. A small, persistent user-profile boundary is needed now so Booking does not
invent or own user state later.

## What Changes

- Add an opt-in MySQL persistence profile to User Service while preserving its Docker-free
  `skeleton` default profile.
- Create the User Service-owned schema, immutable Flyway migration, and bounded user-profile
  APIs for creating, retrieving, updating, and changing a user's booking-eligibility status.
- Enforce unique external user identifiers, optimistic concurrency for mutable profile state,
  safe error envelopes, and MySQL integration verification.
- Keep authentication, credentials, JWT, Gateway, service discovery, Redis, messaging,
  Feign, and any Booking cross-service call outside this increment.

## Capabilities

### New Capabilities

- `user-profile-management`: User Service-owned persistent profile and booking-eligibility
  facts, with bounded HTTP APIs and MySQL verification.

### Modified Capabilities

- `user-service-skeleton`: Extend the User Service dependency and configuration boundary
  with an explicit persistence profile while preserving the standalone skeleton behavior.

## Impact

- Affects `venueflow-user-service`, root dependency management if persistence dependencies
  are not already managed, User Service configuration, Flyway migrations, and its MySQL
  integration-test profile.
- Adds one User Service-owned MySQL schema and no shared entity, cross-service database
  access, secret, external client, or production container orchestration.
