## Context

Gateway, Booking, and Search are the main synchronous pressure boundaries. All services expose
restricted health but have no Prometheus registry or exporter. Traces already use canonical UUIDs
but are not exported.

## Goals / Non-Goals

**Goals:**

- Reject overload safely without converting writes into false success.
- Make timeout ordering executable and testable.
- Export bounded system/business metrics and traces only when explicitly enabled.
- Provide a locally runnable, provisioned observation stack.

**Non-Goals:**

- Invented production thresholds, autoscaling, alert paging, Kubernetes, production HA, or
  performance claims.

## Decisions

1. `stability` is opt-in. Sentinel dependencies are limited to Gateway, Booking, and Search.
   Versioned JSON templates start disabled/zero-threshold and must be filled from later load data.
2. Gateway uses a 5 s response budget, Booking 4 s, and Feign 1 s connect/2 s read budget.
   Tests reject inverted budgets.
3. All executable modules gain Prometheus registry and Micrometer OTel bridge, but default
   management exposure remains health-only and OTLP export is disabled. `observe` exposes
   Prometheus on the protected management surface and enables export.
4. Existing `X-Trace-Id` remains the user-facing correlation ID. W3C trace context is managed by
   Micrometer; resource events retain trace ID in their envelope.
5. Compose `observe` starts Prometheus, Grafana, OTel Collector, and Jaeger with fixed tags,
   local-only ports, bounded resources, and provisioned data sources/dashboards.

## Risks / Trade-offs

- [Bad thresholds reject valid traffic] → shipped rule templates are disabled until evidence.
- [Telemetry outage affects service] → exporters are batch/optional and not readiness-critical.
- [Metrics expose sensitive dimensions] → no token, password, raw query, or unbounded user labels.
- [Observe stack exceeds laptop budget] → explicit profile and bounded memory.

## Migration Plan

Deploy dependencies with `stability/observe` disabled, enable telemetry per service, validate
dashboards, then activate measured Sentinel rules. Roll back by removing the profiles; business
paths and stored facts remain unchanged.

## Open Questions

Concrete production thresholds remain intentionally unset until the v0.8 reproducible load run.
