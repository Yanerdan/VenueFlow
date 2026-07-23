## Why

Resource and User services now own the minimal facts that a later reservation flow will
need, but the repository has no independently verifiable Booking Service boundary. Creating
that boundary now keeps the eventual booking workflow from appearing as an unreviewed
cross-service implementation and preserves the project's Docker-free development baseline.

## What Changes

- Add `venueflow-booking-service` as an executable Spring Boot MVC module in the Maven
  reactor, with an independent `BookingServiceApplication` entry point.
- Establish a secret-free default `skeleton` profile, port `8084`, and restricted Actuator
  liveness/readiness health probes.
- Add Docker-free module and reactor verification for configuration, management exposure,
  random-port HTTP probes, and the packaged executable JAR.
- Enforce a deliberately empty Booking domain boundary: no booking APIs, entities, database,
  migrations, User/Resource clients, authentication, messaging, caching, or infrastructure
  clients.
- Document how to build, run, probe, and verify the Booking Service skeleton.

## Capabilities

### New Capabilities

- `booking-service-skeleton`: Independently executable, infrastructure-free Booking Service
  bootstrap boundary and verification contract.

### Modified Capabilities

<!-- None. -->

## Impact

- Root Maven reactor and a new `venueflow-booking-service` module.
- Service documentation and developer run instructions.
- No database schema, production business API, external infrastructure, or changes to User
  and Resource service contracts.
