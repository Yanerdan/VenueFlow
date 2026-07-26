## Context

C18 issues RS256 access JWTs, but the repository has no public entry service. C19 must establish a
bounded Gateway without coupling business services to Gateway internals or introducing discovery
and resilience concerns at the same time.

## Goals / Non-Goals

**Goals:**

- Add an independently executable WebFlux Gateway on port 8080.
- Route only explicit Auth/User/Resource/Booking path prefixes.
- Validate Auth-issued JWT signature, issuer, and time claims before business routing.
- Replace client identity headers with a trusted JWT subject and propagate one trace ID.
- Bound request size, origins, methods, headers, and external error bodies.
- Preserve connection-free default skeleton startup.

**Non-Goals:**

- Nacos discovery/config, Sentinel, Redis rate limiting, retries, circuit breakers, or load balancing.
- Business authorization, roles, downstream resource-server filters, or token-version lookup.
- Search route, production Compose scheduling, TLS termination, or observability exporters.

## Decisions

### 1. Use a dedicated Spring Cloud Gateway WebFlux module

`venueflow-gateway` uses the managed Gateway WebFlux and OAuth2 Resource Server dependencies, not
Spring MVC or persistence. Port 8080 follows the frozen topology.

### 2. Keep routes explicit and environment-backed

Four programmatic routes map exact path prefixes to bounded base URI properties. Defaults target
local fixed service ports; no discovery locator is enabled. This prevents accidental exposure.

### 3. Validate RS256 locally

The `gateway` profile parses the Auth X.509 public PEM from an untracked environment variable and
creates a reactive JWT decoder with issuer/time validators. Auth and health paths are public;
other requests require authentication. Invalid tokens receive a bounded JSON 401.

### 4. Rebuild trusted context after authentication

Global filters remove incoming `X-User-Id` and `X-Role`. A valid JWT contributes only its `sub`
as `X-User-Id`; no role is synthesized because C18 tokens contain none. A bounded UUID trace ID is
accepted or generated and returned to the caller.

### 5. Test with local bounded HTTP stubs

Default skeleton tests remain external-connection-free. Gateway-profile integration tests use
ephemeral RSA keys and an in-process HTTP stub to prove public routing, 401 behavior, issuer
validation, forged-header removal, trusted-subject forwarding, trace propagation, and bounds.

## Risks / Trade-offs

- [Static routes do not provide failover] → Introduce discovery/load balancing in a later isolated
  change after the entry boundary is stable.
- [Gateway identity headers are not sufficient authorization] → Business services must later
  validate JWTs themselves; C19 makes no authorization claim.
- [Logout does not instantly invalidate an offline Access JWT] → Keep the 15-minute TTL and defer
  token-version enforcement.
- [Content-Length checks cannot bound every streaming attack] → Also cap WebFlux codec buffering;
  production proxy limits remain a deployment concern.

## Migration Plan

1. Register and package the Gateway module.
2. Add skeleton and explicit gateway profiles.
3. Add JWT/CORS/routing/global-filter boundaries and tests.
4. Document local startup and rollout.
5. Roll back by stopping Gateway; direct service behavior and data are unchanged.

## Open Questions

None for C19. Discovery, rate limiting, downstream validation, and TLS are intentionally deferred.
