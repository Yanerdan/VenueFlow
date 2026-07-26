# Secure API Gateway Specification

## Purpose

Define the bounded reactive entry point, Auth-issued JWT validation, trusted request headers, and
deterministic verification boundaries for VenueFlow business APIs.

## Requirements

### Requirement: Gateway is an executable reactive entry module

The repository SHALL provide `venueflow-gateway` as a Spring Cloud Gateway WebFlux module in the
root Maven reactor. It MUST package an executable JAR, default to port 8080, inherit all quality
gates, use no servlet stack, database, business-service implementation dependency, or cross-schema
access, and keep default `skeleton` startup connection-free.

#### Scenario: Default Gateway starts

- **WHEN** the Gateway JAR starts without Docker, keys, or other services
- **THEN** it exposes only restricted health probes on port 8080
- **AND** opens no downstream or infrastructure connection

### Requirement: Gateway exposes only explicit bounded routes

The explicit `gateway` profile SHALL route `/api/v1/auth/**`, `/api/v1/users/**`,
`/api/v1/resources/**`, and `/api/v1/bookings/**` to separately configured bounded HTTP base
URIs. When `governance` is also active, the same allowlist SHALL resolve only the configured Auth,
User, Resource, and Booking service identities through Spring Cloud LoadBalancer. It MUST NOT
enable discovery locator, infer routes, expose search/admin paths, or retry writes automatically.

#### Scenario: A known path is requested

- **WHEN** a request matches one configured prefix
- **THEN** Gateway proxies it only to that route's configured static URI or governance service
  identity

#### Scenario: An unknown path is requested

- **WHEN** a request matches no explicit route or health endpoint
- **THEN** Gateway returns a safe non-success response without a downstream call

### Requirement: Gateway validates Auth-issued access JWTs

Auth APIs and liveness/readiness SHALL remain public. Every business route MUST require an RS256
JWT whose signature matches the configured Auth public key and whose issuer, expiry, not-before
and subject claims are valid. Missing, malformed, expired, wrong-issuer, or invalid-signature
tokens MUST return a bounded JSON 401 and MUST NOT reach a downstream service.

#### Scenario: A valid business request arrives

- **WHEN** a valid Auth-issued JWT accompanies an explicit business route
- **THEN** Gateway authenticates the subject and permits routing

#### Scenario: An invalid token arrives

- **WHEN** JWT verification fails for any reason
- **THEN** Gateway returns `GATEWAY_UNAUTHORIZED` without token or key details
- **AND** no downstream request occurs

### Requirement: Gateway owns trusted identity and trace headers

Gateway MUST remove every client-supplied `X-User-Id` and `X-Role` before routing. For an
authenticated request it SHALL set exactly one `X-User-Id` from JWT `sub` and MUST set no role
header. It SHALL accept only a bounded UUID `X-Trace-Id`, otherwise generate one, propagate it
downstream, return it in the response, and include it in safe error envelopes.

#### Scenario: A caller forges identity headers

- **WHEN** a valid token request also supplies forged user and role headers
- **THEN** downstream receives only the token subject as `X-User-Id`
- **AND** receives no `X-Role`

### Requirement: Gateway bounds browser and request surfaces

Gateway SHALL permit only configured explicit CORS origins, bounded methods and headers, and
credentials only when the origin is explicit. It MUST reject declared request bodies above one
MiB, bound in-memory codec size, and add safe anti-sniffing, frame, cache, and referrer headers.
Tracked configuration MUST contain no usable secret or private key.

#### Scenario: An oversized request is declared

- **WHEN** Content-Length exceeds the configured maximum
- **THEN** Gateway returns 413 before routing

#### Scenario: A disallowed browser origin sends a preflight

- **WHEN** Origin is outside the configured allowlist
- **THEN** Gateway emits no permissive CORS authorization

### Requirement: Gateway verification remains deterministic

Default verification SHALL cover skeleton startup, dependency/architecture boundaries, restricted
health, executable packaging, route matching, JWT validation, identity/trace replacement, CORS,
request bounds, and safe errors without Docker or external services.

#### Scenario: Root verification runs

- **WHEN** root `clean verify` runs
- **THEN** Gateway tests use only isolated ports, in-memory keys, and local bounded stubs
- **AND** no database, Nacos, Redis, Sentinel, or remote service is contacted

### Requirement: C19 preserves milestone boundaries

C19 MUST NOT add Nacos, Feign, Sentinel, Redis rate limiting, tracing exporters, database access,
business authorization, roles, service-owned JWT filters, search, production Compose application
containers, or automatic retry/circuit-breaker behavior.

#### Scenario: C19 scope is inspected

- **WHEN** code, dependencies, configuration, tests, and deployment files are reviewed
- **THEN** changes remain limited to Gateway entry routing/security and documentation
