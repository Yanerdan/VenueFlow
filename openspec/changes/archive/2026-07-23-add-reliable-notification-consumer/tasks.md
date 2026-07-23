## 1. Dependencies, profiles, and schema

- [x] 1.1 Audit C11 envelope/publication code and C12 skeleton/configuration tests before editing.
- [x] 1.2 Add only the approved validation, persistence, AMQP, MySQL, and isolated test
  dependencies; update Enforcer rules and add the explicit `consumer-it` profile.
- [x] 1.3 Add fail-fast `persistence` and `messaging` configuration with environment-only
  credentials and bounded queue/listener/retry/confirm settings.
- [x] 1.4 Add immutable Notification V001 for consumed events, notification records, and bounded
  failure facts with required uniqueness/check/size indexes and no cross-schema references.

## 2. Envelope, notification, and transaction boundary

- [x] 2.1 Implement bounded C11 envelope parsing, exact route/type/version validation, canonical
  SHA-256 hashing, and stable failure classification.
- [x] 2.2 Implement deterministic confirmation/cancellation in-app notification derivation using
  only typed bounded Booking facts.
- [x] 2.3 Implement Notification-owned repositories and one short transaction that inserts
  consumed identity plus exactly one notification.
- [x] 2.4 Implement exact-duplicate verification, identity-collision rejection, and bounded
  failure/replay audit persistence without raw messages or exceptions.

## 3. RabbitMQ consumption and failure transfer

- [x] 3.1 Declare the durable source binding, work queue, fixed-delay retry topology, dead
  exchange/queue, exact routing keys, persistent messages, and validated consumer ownership.
- [x] 3.2 Implement a bounded manual-ACK listener that commits local facts before ACK and maps
  exact duplicates to safe ACK without another side effect.
- [x] 3.3 Implement persistent retry/dead-letter republish with bounded attempt headers,
  mandatory routing, Publisher Confirm/Return, ACK-after-transfer, and NACK/requeue on uncertain
  transfer.
- [x] 3.4 Add safe internal metrics/logs for receive, consume, duplicate, retry, dead-letter,
  replay, ACK/NACK, outcomes, depth, and oldest age.

## 4. Controlled dead-letter replay

- [x] 4.1 Add a non-HTTP application command that previews only bounded metadata for the next DLQ
  message and leaves it available.
- [x] 4.2 Require expected identity/fingerprint, bounded reason, and explicit confirmation before
  replay; confirm routing before ACKing the DLQ source and reset only the attempt header.
- [x] 4.3 Verify replay duplicates remain harmless through the same consumed-event boundary and
  that preview/replay logs contain no body, notification text, credential, or endpoint.

## 5. Docker-free and real-infrastructure verification

- [x] 5.1 Add deterministic unit tests for envelope bounds, canonical hashes, notification
  derivation, duplicate/collision decisions, retry classification, and attempt limits.
- [x] 5.2 Add transaction, ACK/NACK, confirm/return, interruption, replay-guard, configuration,
  architecture, and secret/path tests using ports/fakes only.
- [x] 5.3 Preserve default skeleton context/JAR/health verification and prove root/module
  `clean verify` creates no datasource, broker connection, listener, or Testcontainers fixture.
- [x] 5.4 Add `consumer-it` MySQL 8.4 evidence for V001, constraints, atomic notification commit,
  duplicate delivery, collision handling, and rollback.
- [x] 5.5 Add `consumer-it` RabbitMQ 4.1.8 evidence for durable exact routing, manual ACK,
  commit-before-ACK redelivery, fixed-delay retry, poison DLQ, confirmed replay, and broker
  recovery.

## 6. Documentation and final gates

- [x] 6.1 Update module/root README and add a Notification consumer runbook covering profiles,
  topology, credentials, C11 requeue order, metrics, DLQ preview/replay, shutdown, and rollback.
- [x] 6.2 Update HANDOFF with exact default/`consumer-it` commands, test evidence, known
  at-least-once crash windows, and scoped diff inventory.
- [x] 6.3 Run module/root verification, `consumer-it`, Enforcer, Spotless, SpotBugs, SBOM,
  migration immutability, secret/path, dependency, service-boundary, and `git diff --check`
  gates.
- [x] 6.4 Run strict OpenSpec validation and confirm C13 changes no Booking code/migration,
  production Compose application, email/public API, timeout cancellation, or cross-service
  database access.
