## Why

VenueFlow now has the complete business, messaging, governance, cache, and search chain, but
operators cannot quantify saturation or contain overload. This change completes v0.7 with opt-in
stability controls and end-to-end telemetry.

## What Changes

- Add opt-in Sentinel protection for Gateway, Booking, and Search with versioned rule templates
  and explicit rejection/degradation semantics.
- Enforce the Gateway > Booking > collaborator timeout budget in tracked configuration/tests.
- Add Prometheus metrics and OpenTelemetry trace export to executable modules under an explicit
  `observe` profile, preserving connection-free defaults.
- Propagate trace context through HTTP/Feign/resource events and expose bounded business metrics.
- Add opt-in Prometheus, Grafana, OTel Collector, and Jaeger Compose services plus provisioned
  dashboards and an operations runbook.

## Capabilities

### New Capabilities

- `service-stability-controls`: overload rejection, circuit boundaries, timeout budgets, and
  versioned Sentinel rules.
- `system-observability`: safe metrics, trace propagation/export, health semantics, and
  provisioned dashboards.

### Modified Capabilities

None.

## Impact

Gateway, Booking, Search, Resource event publication, all executable service dependencies/config,
Compose/image locks, dashboards, collector configuration, tests, environment examples, README,
and HANDOFF are updated. No business schema or API success contract is changed.
