## ADDED Requirements

### Requirement: Resource changes are published from a transactional Outbox

Resource create/status transactions SHALL append a versioned `resource.changed.v1` Outbox record
with resource ID, aggregate version, event ID, occurrence time, producer, and trace identity.
Publication SHALL require publisher confirm and tolerate duplicate delivery.

#### Scenario: Broker is unavailable after a resource update

- **WHEN** MySQL commits but RabbitMQ publication fails
- **THEN** the resource remains committed and the Outbox record remains retryable

### Requirement: Search owns only a rebuildable projection

Search Service SHALL own no resource fact database. Its Elasticsearch document SHALL be replaced
only by an equal/newer resource version obtained from Resource Service, and duplicate or stale
events MUST NOT regress the projection.

#### Scenario: Events arrive out of order

- **WHEN** a lower-version event follows a higher-version document
- **THEN** Search retains the higher-version projection

### Requirement: Search API is bounded and explicitly degraded

`GET /api/v1/search/resources` SHALL accept only bounded text/category/status/page/page-size
inputs, cap page size at 100 and the result window at 10,000, and return typed results.
Elasticsearch failure MUST return
`SEARCH_UNAVAILABLE` and MUST NOT masquerade as an empty successful result.

#### Scenario: Elasticsearch is down

- **WHEN** a user performs a search
- **THEN** the API returns a bounded non-success response with trace identity

### Requirement: Projection consumption is reliable

The Search consumer SHALL use a durable queue, manual acknowledgement, finite retry/dead-letter
handling, and inbox uniqueness by consumer/event ID. It SHALL fetch the latest Resource snapshot
before indexing and acknowledge only after durable projection/inbox completion.

#### Scenario: A message is delivered twice

- **WHEN** the same event reaches Search again
- **THEN** inbox identity prevents duplicate side effects and the message is acknowledged

### Requirement: Search supports safe full rebuild and alias switch

An explicit administrative rebuild SHALL create a new physical index, import bounded Resource
pages, validate the indexed count, and atomically move `venueflow-resource-read` and
`venueflow-resource-write` aliases. A failed rebuild MUST leave the current read alias intact.

#### Scenario: Rebuild fails before validation

- **WHEN** snapshot import or count validation fails
- **THEN** users continue searching the previous aliased index

### Requirement: Search remains optional and deterministically verified

Default Search startup and root verification SHALL require no Elasticsearch, RabbitMQ, Nacos,
Redis, or database. Tests SHALL cover mapping, filtering, version ordering, duplicate delivery,
failure degradation, rebuild validation, and alias switching with in-process boundaries.

#### Scenario: Default Search starts

- **WHEN** the executable JAR starts with its default profile
- **THEN** restricted health probes become available without external connections
