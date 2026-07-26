## 1. Resource cache

- [x] 1.1 Add opt-in Redis dependency/configuration and connection-free defaults
- [x] 1.2 Implement Cache Aside, bounded TTL/keys, negative cache, failure fallback, and rebuild lock
- [x] 1.3 Add post-commit invalidation and deterministic cache tests

## 2. Resource change publication

- [x] 2.1 Add Resource V004 Outbox migration and transactional event append
- [x] 2.2 Add confirmed publisher, retry state, RabbitMQ topology, and tests

## 3. Search service

- [x] 3.1 Add executable connection-free Search module and governance configuration
- [x] 3.2 Implement bounded search documents/API and explicit Elasticsearch degradation
- [x] 3.3 Implement inbox-idempotent resource event projection with latest snapshot/version checks
- [x] 3.4 Implement full rebuild, validation, and atomic read/write alias switch

## 4. Routing and deployment

- [x] 4.1 Add explicit static/governed Gateway search route
- [x] 4.2 Lock Elasticsearch 9.2.8 and add opt-in Compose search profile
- [x] 4.3 Update environment examples, Nacos data ID, README, runbook, and HANDOFF

## 5. Verification and completion

- [x] 5.1 Run module/root tests and dependency, scope, credential, and diff gates
- [x] 5.2 Record optional Redis/Elasticsearch integration smoke result (registry pull timed out;
  Compose and protocol-level client checks passed)
- [x] 5.3 Strictly validate, sync, archive, commit, and push the change
