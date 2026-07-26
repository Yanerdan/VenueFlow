## ADDED Requirements

### Requirement: Resource detail cache is optional and non-authoritative

Resource Service SHALL enable Redis caching only with an explicit `cache` profile. Default startup
MUST NOT connect to Redis, and cache failure MUST fall back to the MySQL-owned resource fact.

#### Scenario: Redis is unavailable

- **WHEN** a resource detail request cannot read or write Redis
- **THEN** the service returns the bounded MySQL result or its normal domain error

### Requirement: Cache Aside bounds staleness and stampedes

Resource detail hits SHALL use a bounded jittered TTL, misses SHALL use a shorter TTL, and
concurrent misses for one key SHALL use per-key rebuild exclusion. Keys MUST include project and
environment prefixes and MUST contain no sensitive value.

#### Scenario: Concurrent requests miss one key

- **WHEN** multiple requests concurrently read the same uncached resource
- **THEN** only one local rebuild queries MySQL and followers observe its result

### Requirement: Resource writes invalidate after commit

Successful resource create/status changes SHALL evict the affected detail only after the owning
database transaction commits. Eviction failure MUST NOT roll back a committed resource change.

#### Scenario: Resource status changes

- **WHEN** a status transaction commits
- **THEN** its cache key is invalidated and the next miss reloads the latest version

### Requirement: Cache verification is deterministic

Default tests SHALL prove hit, miss, negative caching, concurrent rebuild, invalidation, failure
fallback, TTL/key bounds, and connection-free profile behavior without a live Redis server.

#### Scenario: Root verification runs

- **WHEN** root `clean verify` executes
- **THEN** cache behavior uses deterministic in-process fakes and opens no Redis connection
