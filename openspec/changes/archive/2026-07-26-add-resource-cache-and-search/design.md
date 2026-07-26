## Context

Resource Service owns catalog facts in MySQL. RabbitMQ reliability patterns already exist in
Booking/Notification, while Redis is deployed but unused and Search Service does not exist.
Default verification must remain independent of Docker.

## Goals / Non-Goals

**Goals:**

- Reduce repeated resource-detail database reads without making Redis authoritative.
- Reliably project resource changes into a searchable Elasticsearch index.
- Support bounded incremental consumption, full rebuild, alias switch, and explicit degradation.
- Preserve connection-free defaults and service/database ownership.

**Non-Goals:**

- Redis-backed capacity, distributed transactions, recommendation/ranking, arbitrary ES DSL,
  production clustering, UI, Sentinel, observability stack, or release materials.

## Decisions

1. Resource cache is activated only by `cache`; Cache Aside wraps detail reads. Values and misses
   have separate bounded TTLs with jitter. A JVM per-key lock prevents local stampedes; Redis
   remains optional and failures fall back to MySQL.
2. Resource writes append `resource.changed.v1` to a Resource-owned Outbox in the same transaction.
   The existing publisher-confirm pattern is reused. Events carry resource ID/version; Search
   fetches the latest Resource HTTP snapshot so old events cannot overwrite new facts.
3. Search is a separate MVC module. Default `skeleton` has no external connections; `search`
   enables AMQP and Elasticsearch 9.2.8. Documents use version-based overwrite and a fixed read
   alias. Search accepts bounded typed parameters only.
4. Rebuild creates a versioned physical index, pages Resource snapshots through a bounded Feign
   client, validates counts, then atomically moves the read/write aliases. It never mutates MySQL.
5. Event consumption is manual-ack and inbox-idempotent. ES failure rejects for bounded broker
   retry/dead-letter processing; search API returns `SEARCH_UNAVAILABLE`, never an empty success.
6. Gateway receives one explicit search route. Discovery locator remains disabled.

Alternatives rejected: dual writes to ES (unrecoverable ambiguity), Redis as truth, exposing raw
ES query DSL, and adding search behavior to Resource Service.

## Risks / Trade-offs

- [Short stale-cache window] → bounded jittered TTL plus post-commit eviction.
- [Event duplicated or reordered] → inbox identity plus document version comparison/latest fetch.
- [ES downtime grows backlog] → durable queue, finite retries/DLX, replay and rebuild operations.
- [Rebuild races with incremental writes] → separate physical index and atomic alias switch.
- [Extra operational footprint] → explicit `search` profile and 768 MiB single-node limit.

## Migration Plan

1. Apply Resource V004 Outbox migration and deploy Resource with messaging disabled.
2. Start Redis optionally and enable `cache`.
3. Start Elasticsearch/Search, create aliases, then enable Resource event publication.
4. Rebuild once and enable the Gateway search route.
5. Roll back by disabling cache/search/event profiles; MySQL facts remain intact.

## Open Questions

None blocking. Relevance tuning and multilingual analyzers are deferred until real usage exists.
