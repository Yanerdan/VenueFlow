## Context

All core services and the Gateway are executable, but route/collaborator addresses are static.
Default verification is deliberately infrastructure-free, so governance must remain opt-in and
must not make Nacos availability a prerequisite for ordinary builds.

## Goals / Non-Goals

**Goals:**

- Close v0.3 with Nacos discovery/config, registered Gateway routes, bounded Feign calls, two
  Resource instances, failover evidence, and trace propagation.
- Preserve existing static persistence and gateway modes for deterministic local tests.
- Make all timeouts and retry boundaries explicit; never retry capacity writes automatically.

**Non-Goals:**

- Redis, Sentinel, Elasticsearch, Kubernetes, business authorization, schema changes, or dynamic
  exposure of arbitrary registered services.

## Decisions

- Use a separate `governance` profile layered on existing runtime profiles. Skeleton remains the
  default and explicitly disables discovery/config clients.
- Add Nacos discovery/config starters only to executable application modules. Config imports are
  `optional:nacos:` and contain no secret values.
- Gateway governance routes remain an explicit allowlist but use `lb://` service identities.
- Booking governance mode uses OpenFeign interfaces and adapters. Existing Java HTTP adapters stay
  active for `persistence & !governance`, avoiding a breaking migration and preserving isolated ITs.
- Feign uses fixed connect/read timeouts, retryer `NEVER_RETRY`, safe error translation, and a
  request interceptor that forwards only a validated UUID trace from MDC.
- Validate instance selection/failover with an in-memory service-instance supplier and isolated
  local servers; live Nacos smoke remains opt-in.

## Risks / Trade-offs

- [Nacos starter auto-configuration may contact infrastructure unexpectedly] → Disable discovery
  and config by default and test skeleton executable startup.
- [A failed write may have succeeded remotely] → Keep writes non-retrying and preserve operation
  lookup/reconciliation semantics.
- [Large cross-module dependency edit] → Enforcer allowlists, architecture tests, module tests, and
  root verification protect boundaries.
- [Optional config import can hide an unavailable control plane] → Governance readiness and the
  smoke runbook expose registration state; services retain cached/local configuration.

## Migration Plan

Deploy Nacos configuration, start service instances with existing runtime profiles plus
`governance`, then start Gateway governance mode. Roll back by removing `governance`; static URI
profiles remain operational and database/event contracts are unchanged.

## Open Questions

None. Sentinel and production deployment policy remain later milestones.
