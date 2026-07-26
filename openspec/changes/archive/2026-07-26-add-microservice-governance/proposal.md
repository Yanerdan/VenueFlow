## Why

VenueFlow now has a secure Gateway and complete core services, but service locations remain static
and Booking uses hand-built HTTP clients. The next milestone closes the v0.3 governance gap in one
large, independently verifiable change.

## What Changes

- Add an opt-in `governance` profile for Nacos discovery and non-secret config import across
  Gateway and the five application services.
- Route Gateway by registered service identity and use Spring Cloud LoadBalancer.
- Replace Booking's governance-mode User/Resource calls with bounded OpenFeign clients while
  retaining the static HTTP persistence mode for isolated tests and local fallback.
- Propagate a validated UUID `X-Trace-Id` through Gateway, MVC services, Feign, and responses.
- Add deterministic two-instance Resource load-balancing and one-instance failure tests without
  requiring a live Nacos server in the default build.
- Keep default skeleton startup connection-free and secrets environment-only.

## Capabilities

### New Capabilities

- `microservice-governance`: Nacos registration/configuration, bounded service-to-service clients,
  instance failover, and trace propagation.

### Modified Capabilities

- `secure-api-gateway`: Governance mode resolves explicit routes through registered service
  identities while preserving the existing static gateway mode.

## Impact

Gateway, Auth, User, Resource, Booking, and Notification configuration and dependencies are
affected. Booking gains governance-only Feign adapters; tests, environment documentation, runbook,
and HANDOFF are updated. No database schema, business API, event contract, Redis, Sentinel, or
Elasticsearch behavior changes.
