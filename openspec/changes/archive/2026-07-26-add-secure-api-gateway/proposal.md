## Why

Auth now issues verifiable identities, but clients still reach business services directly and no
single boundary validates access JWTs or strips forged identity headers. C19 adds the smallest
secure Gateway slice before discovery, resilience, and downstream authorization.

## What Changes

- Add `venueflow-gateway` as an executable Spring Cloud Gateway WebFlux module on port 8080.
- Add explicit static routes for Auth, User, Resource, and Booking; do not auto-expose services.
- Keep Auth APIs and liveness/readiness public, and require a valid Auth-issued RS256 JWT for
  business routes.
- Strip client `X-User-Id`/`X-Role`, derive `X-User-Id` only from JWT `sub`, and propagate a bounded
  trace ID.
- Add bounded request size, explicit CORS, security headers, and safe JSON authentication errors.
- Keep default skeleton startup connection-free; secure routing is enabled only by `gateway`.
- Exclude Nacos, Sentinel, Redis rate limiting, tracing exporters, search routing, service
  authorization, and application-container deployment.

## Capabilities

### New Capabilities

- `secure-api-gateway`: Defines explicit routing, RS256 entry validation, trusted user context,
  trace propagation, CORS/request bounds, safe errors, and verification.

### Modified Capabilities

None.

## Impact

- Adds one Maven reactor module and Gateway-specific environment examples/documentation.
- Reads the existing Auth public key but adds no database, migration, secret, or cross-service
  implementation dependency.
- Existing services and routes remain unchanged when Gateway is not running.
