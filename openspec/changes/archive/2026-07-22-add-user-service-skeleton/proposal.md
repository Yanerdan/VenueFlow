## Why

VenueFlow has a complete Resource-side availability foundation, but it has no independent
User Service process on which future user profile, booking eligibility, and authentication
work can safely build. Establishing the same small, runnable service boundary used by
Resource Service keeps later identity work incremental and independently verifiable.

## What Changes

- Add `venueflow-user-service` as an executable Spring Boot MVC module in the Maven reactor.
- Provide a secret-free default skeleton profile, explicit `SERVER_PORT` configuration with
  a User Service default port, and restricted Actuator liveness/readiness probes.
- Add Docker-free module and reactor verification for startup, health exposure, packaging,
  and port override behavior.
- Keep user persistence, registration/login, JWT, roles, authorization, Gateway, Nacos,
  Redis, RabbitMQ, Feign, and Booking outside this increment.

## Capabilities

### New Capabilities

- `user-service-skeleton`: Runnable, infrastructure-independent User Service module with
  safe health endpoints and deterministic configuration.

### Modified Capabilities

- None.

## Impact

- Affects the root Maven reactor, `venueflow-user-service` source/configuration/tests, and
  engineering documentation describing the independently runnable service.
- Adds no database schema, external service client, tracked secret, production container
  orchestration, or cross-service API contract.
