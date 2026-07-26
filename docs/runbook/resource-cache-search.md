# Resource cache and search runbook

## Boundaries

- MySQL in Resource Service is authoritative.
- Redis values are disposable and have bounded TTLs.
- Elasticsearch indices are projections and can be rebuilt.
- Default `skeleton` profiles contact none of these systems.

## Start infrastructure

```powershell
docker compose --env-file deploy/versions.env --env-file .env `
  -f deploy/compose/compose.yml --profile base --profile search up -d
```

Start Resource with `persistence,cache,resource-events` and Search with `search` (add `governance`
only when Nacos is desired). Credentials remain in `.env`.

## Rebuild

Call `POST /api/v1/admin/search/rebuild` directly on port 8086 from the operator network. The job
creates a physical index, pages Resource snapshots, checks the exact count, then atomically moves
`venueflow-resource-read` and `venueflow-resource-write`. A failure before alias switching leaves
the old index live.

## Failure handling

- Redis down: Resource detail reads fall back to MySQL; bounded TTL repairs missed eviction.
- RabbitMQ down: committed changes remain `NEW/RETRY` in `resource_outbox`.
- Elasticsearch down: Search returns `503 SEARCH_UNAVAILABLE`; messages dead-letter rather than
  being acknowledged as indexed.
- Duplicate/out-of-order events: inbox identity and external document versioning prevent
  regression.

After recovery, requeue audited dead letters or run a full rebuild. Never edit projection
documents as business facts and never delete MySQL/Redis/ES volumes from automation.

## Verification

```powershell
.\mvnw.cmd -pl venueflow-resource-service,venueflow-search-service,venueflow-gateway -am clean verify
openspec.cmd validate add-resource-cache-and-search --strict
```

The image/client line is pinned to Elasticsearch 9.2.8. If the Elastic registry is unavailable,
record the pull failure and rerun only the optional live smoke; deterministic adapter and root
verification remain mandatory.
