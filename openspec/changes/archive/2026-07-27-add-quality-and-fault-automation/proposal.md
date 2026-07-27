## Why

VenueFlow has business and observability coverage, but v0.8 still lacks one repeatable entry point
for repository policy checks and safe fault exercises. Automating those checks now prevents manual
runbooks from becoming unverifiable claims.

## What Changes

- Add deterministic repository quality checks for credentials, mutable image tags, migrations,
  Compose profile boundaries, and OpenSpec consistency.
- Add dry-run-by-default fault scenarios for consumer, Elasticsearch, Resource instance,
  downstream latency, duplicate event, Outbox publisher, and Redis failure boundaries.
- Add explicit preconditions, bounded execution, recovery commands, evidence manifests, and
  rollback verification without claiming unexecuted results.
- Run the deterministic policy gate in CI; keep live fault execution opt-in.

## Capabilities

### New Capabilities

- `quality-fault-automation`: deterministic quality policy, safe fault injection, recovery, and
  evidence contracts.

### Modified Capabilities

None.

## Impact

CI, repository scripts, fault scenario descriptors, operations documentation, README, and HANDOFF
are updated. No business API, schema, runtime dependency, or production threshold changes.
