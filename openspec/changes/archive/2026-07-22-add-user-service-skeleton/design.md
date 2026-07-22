## Context

The Maven reactor currently contains a single executable business service:
`venueflow-resource-service`. Resource-side catalog, slots, and capacity allocation are
complete, but no independently runnable User Service boundary exists. Future User profile,
eligibility, Auth, and Booking work needs a service-owned starting point without introducing
premature persistence, security, or distributed infrastructure.

## Goals / Non-Goals

**Goals:**

- Add a small `venueflow-user-service` Spring Boot MVC executable module to the existing
  Maven reactor.
- Make the default process start on port 8082 without Docker or any external dependency.
- Expose only liveness/readiness health probes and verify packaging, startup, port override,
  and endpoint restrictions automatically.
- Preserve the same secret-safe configuration and quality-gate conventions used by Resource
  Service.

**Non-Goals:**

- User Entity/table/Migration, registration, credentials, login, JWT, refresh tokens,
  roles, eligibility rules, or authorization.
- Gateway, Nacos, Redis, RabbitMQ, Feign, Sentinel, tracing, Prometheus, Elasticsearch,
  Docker application containers, or any cross-service calls.

## Decisions

### Mirror the executable-module pattern without copying business behavior

User Service will be a sibling Maven module with its own `main` class, configuration, and
tests. It will reuse parent dependency management and quality plugins, but it will not reuse
Resource business packages or persistence configuration. This gives the new service a stable
ownership boundary while avoiding a speculative shared domain abstraction.

### Keep the default profile infrastructure-independent

The default `skeleton` profile will set `spring.application.name=venueflow-user-service` and
derive the listening port from `SERVER_PORT`, defaulting to `8082`. No datasource or external
client will be configured. A future persistence Change can add an explicit profile and
environment-supplied credentials without altering this default behavior.

### Restrict management endpoints from the first runnable version

Actuator Web will expose only `/actuator/health/liveness` and `/actuator/health/readiness`.
The service will not anonymously expose configuration, mappings, metrics, loggers, or write
operations. This is selected over a permissive development actuator surface because endpoint
restriction is easier to preserve than to retrofit.

## Risks / Trade-offs

- [A skeleton has no visible user feature] → It is deliberately small; the next User-domain
  Change will add persistence and APIs on this verified boundary.
- [Duplicated service bootstrap configuration] → The modules share dependency and quality
  policy only; service configuration stays explicit until a demonstrated common abstraction
  is justified.
- [Port 8082 can already be occupied locally] → `SERVER_PORT` is overridable and automated
  tests use random ports.

## Migration Plan

1. Add the module to the Maven reactor and run default full-reactor verification.
2. Start the standalone jar with no infrastructure and probe the health allowlist.
3. Deploy only as a development skeleton; no user data migration or compatibility concern
   exists in this change.
4. Roll back by removing the newly added module; no persisted data or external contract is
   introduced.

## Open Questions

- The next User-domain Change will decide profile fields, status/eligibility lifecycle,
  database schema, and whether Auth reads an internal contract or shares no runtime storage.
