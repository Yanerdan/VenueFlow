## Why

VenueFlow now has a complete reservation lifecycle but no authentication boundary. Before login,
JWT, or Gateway work, the repository needs an independently executable Auth Service with the same
bounded startup and verification baseline as the existing services.

## What Changes

- Add `venueflow-auth-service` to the Maven reactor as a Spring Boot executable on port 8081.
- Provide skeleton-only startup with liveness/readiness health endpoints and stable service
  identity.
- Add architecture, configuration, packaging, health, and dependency-boundary verification.
- Document startup, environment overrides, and explicit C17 non-goals.
- Do not add credentials, login APIs, JWT keys/tokens, persistence, Gateway, Nacos, messaging,
  caching, or cross-service calls.

## Capabilities

### New Capabilities

- `auth-service-skeleton`: Defines the executable Auth Service boundary, safe default startup,
  health surface, dependency limits, and Docker-free verification.

### Modified Capabilities

None.

## Impact

- Adds one Maven module and registers it in the root reactor.
- Updates root/module documentation and handoff state.
- Adds no database migration, external API, infrastructure connection, secret, or deployment
  container.
