## Why

VenueFlow already contains Nacos, governance profiles, discovered Gateway routes, Feign clients,
and tracked configuration, but the normal local workflow cannot initialize or prove them. This
leaves the portfolio claiming a governance capability that is difficult to reproduce and makes
multi-instance failure behavior dependent on manual setup.

## What Changes

- Add an explicit, non-breaking governed local startup mode while preserving the current static
  URI startup as the default fallback.
- Initialize authenticated Nacos 3 safely, publish tracked non-secret Data IDs, and verify them.
- Register all services and start two Resource instances with distinct ports and instance IDs.
- Extend local status and smoke tooling to prove registration, discovered routing, trace
  propagation, and continued service after one Resource instance stops.
- Disable unintended OTLP metric publication unless the explicit `observe` profile is active.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `microservice-governance`: Require a repeatable governed local startup and bounded live
  registration/failover verification path.
- `system-observability`: Require all OTLP metrics and traces to remain disabled outside the
  explicit observation profile.

## Impact

- Local start, stop, status, Nacos bootstrap, and governance smoke scripts.
- Nacos Compose authentication settings and local environment generation.
- Service profile composition and multi-instance PID/log management.
- Default and observation management configuration across executable modules.
- Governance and observability documentation and deterministic tests.
