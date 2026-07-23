## Why

C11 can reliably publish Booking events, but no consumer-owned service exists yet. Before adding
manual acknowledgement, inbox deduplication, retry, dead-letter handling, or notification
delivery, VenueFlow needs an independently executable Notification Service with the same
connection-free default and quality boundaries as the existing services.

## What Changes

- Add `venueflow-notification-service` as an executable Spring Boot MVC reactor module on port
  `8085`.
- Add a secret-free `skeleton` profile that exposes only restricted liveness/readiness health
  probes and creates no database, RabbitMQ, collaborator, or outbound notification connection.
- Limit dependencies to the existing Web MVC, Actuator, and test/build support required for the
  skeleton.
- Add deterministic context, HTTP health, executable-JAR, configuration, dependency, and
  architecture verification.
- Document the module boundary and reserve explicit future profiles for Notification persistence
  and RabbitMQ consumption without implementing them in C12.
- Keep queues, bindings, `ConsumedEvent`, manual ACK, retries, dead letters, email simulation, and
  notification records outside this Change.

## Capabilities

### New Capabilities

- `notification-service-skeleton`: Independently executable, infrastructure-free Notification
  Service bootstrap, restricted health surface, dependency boundary, and verification contract.

### Modified Capabilities

None.

## Impact

- Root Maven reactor: one new service module.
- `venueflow-notification-service`: application entry point, minimal configuration, tests, and
  README.
- Dependencies: existing centrally managed Spring Boot Web MVC, Actuator, and test support only.
- Runtime: default port `8085`; no Compose application container or infrastructure connection.
- Future work: C13 can add Notification-owned persistence and reliable RabbitMQ consumption
  without weakening the C12 default skeleton boundary.
