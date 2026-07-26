## Why

VenueFlow already has MySQL-owned resource facts and reliable messaging, but resource reads still
hit MySQL and users cannot search. This change completes the v0.6 milestone while preserving
MySQL as truth and keeping Redis/Elasticsearch optional.

## What Changes

- Add opt-in Redis Cache Aside for resource details with bounded TTL, negative caching, jitter,
  and per-key rebuild protection.
- Publish versioned resource-change events through a Resource-owned transactional Outbox.
- Add an executable Search Service with Elasticsearch projection, bounded search API, idempotent
  event consumption, explicit degradation, and rebuild/alias operations.
- Add an explicit Gateway search route and opt-in Redis/Elasticsearch Compose profiles.
- Keep default builds and skeleton startup connection-free; verify failure/recovery with
  deterministic fixtures and integration tests where infrastructure is available.

## Capabilities

### New Capabilities

- `resource-cache`: Redis Cache Aside and invalidation boundaries for resource reads.
- `resource-search-projection`: searchable, rebuildable Elasticsearch projection fed by reliable
  resource-change events.

### Modified Capabilities

- `secure-api-gateway`: add only the explicit `/api/v1/search/**` route.

## Impact

Resource Service gains scoped Redis and AMQP/Outbox behavior under explicit profiles. A new
`venueflow-search-service` module owns only Elasticsearch projection state and its API. Gateway,
Compose, locked dependency/image versions, environment examples, tests, runbooks, root reactor,
and governance configuration are updated.
