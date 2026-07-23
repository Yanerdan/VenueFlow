## 1. Restore change and repository alignment

- [x] 1.1 Audit every existing uncommitted C10 file against the corrected design and record
  retain/rework/replace decisions without deleting unrelated user work.
  - Acceptance: the design audit table and actual file inventory agree; no task is credited only
    because a file exists.
  - Test: `git status --short` and scoped diff review.
- [x] 1.2 Update Booking dependency enforcement and explicit persistence configuration while
  preserving secret-free Docker-free `skeleton` startup.
  - Acceptance: only approved validation, MyBatis-Plus, Flyway, MySQL, and test dependencies are
    added; DB/base URLs/timeouts come from environment or safe defaults as designed.
  - Test: dependency-tree/enforcer checks plus Booking skeleton context test.

## 2. Resource operation-result contract

- [x] 2.1 Add a Resource application query and DTO-only
  `GET /api/v1/resource-slots/{slotId}/allocation-operations/{operationId}` endpoint over the
  existing V003 ledger.
  - Acceptance: matching operation returns bounded facts; missing or slot-mismatched operation
    returns the established safe error envelope; no schema or migration changes.
  - Test: Resource application and HTTP tests for found, missing, and wrong-slot cases.
- [x] 2.2 Extend default and opt-in Resource verification for operation lookup.
  - Acceptance: Docker-free tests cover the contract and `mysql-it` proves lookup against the
    existing V003 table without modifying V001-V003.
  - Test: Resource module `test` and explicit Resource `mysql-it` suite.

## 3. Booking persistence and atomic idempotency

- [x] 3.1 Replace the unreleased Booking migration with immutable
  `V001__init_booking_reservations.sql` containing scoped idempotency and reservation facts.
  - Acceptance: unique `(user_id, operation, idempotency_key)`, unique request/booking/allocation
    IDs, positive quantity, valid status checks, versions, timestamps, and only Booking-owned
    tables exist.
  - Test: SQL/static migration review and opt-in fresh-MySQL migration test.
- [x] 3.2 Implement Booking domain models, request hashing, repository ports, and short
  transactional claim/finalization operations.
  - Acceptance: one concurrent executor obtains `PROCESSING`; matching replay, conflict,
    succeeded, failed, and in-progress outcomes are deterministic; no HTTP call occurs inside a
    DB transaction.
  - Test: unit/repository tests including concurrent unique-constraint arbitration.
- [x] 3.3 Implement `CONFIRMED -> CANCELLED` optimistic lifecycle without exposing persistence
  Entities outside infrastructure.
  - Acceptance: stale or illegal updates cannot overwrite state; cancelled replay is stable.
  - Test: domain, repository, and architecture/boundary tests.

## 4. Bounded collaborator adapters

- [x] 4.1 Implement typed `UserEligibilityClient` and `ResourceCapacityClient` ports/adapters
  with configurable connect/request timeouts and safe failure mapping.
  - Acceptance: no Feign/discovery dependency, no string-built JSON, no write retry, interrupt
    status is preserved, and collaborator bodies/secrets are not leaked.
  - Test: HTTP-stub tests for success, rejection, malformed response, timeout, and interruption.
- [x] 4.2 Implement Resource allocation-result lookup and bounded ambiguous-timeout resolution.
  - Acceptance: proven allocation continues, definitive not-found fails without Booking
    persistence, unresolved outcome returns its distinct error, and allocation is never resent.
  - Test: HTTP-stub sequence tests and application orchestration tests.

## 5. Reservation orchestration

- [x] 5.1 Implement idempotent create orchestration: claim, eligibility, allocation, timeout
  resolution, final Booking transaction, and replay.
  - Acceptance: concurrent identical requests call User/Resource once effectively and return one
    reservation; different payload reuse returns conflict before collaborator calls.
  - Test: application concurrency/replay/conflict tests with controlled stubs.
- [x] 5.2 Implement deterministic compensation for local finalization failure.
  - Acceptance: one release attempt uses the stored deterministic release ID; success,
    already-released, failed, and ambiguous compensation outcomes map to distinct safe errors and
    logs.
  - Test: compensation matrix tests.
- [x] 5.3 Implement idempotent cancellation with Resource release before conditional local
  transition.
  - Acceptance: release failure leaves Booking `CONFIRMED`; concurrent/replayed cancellation
    yields one effective release and one `CANCELLED` fact.
  - Test: cancellation replay, concurrent cancel, downstream failure, and stale-version tests.

## 6. HTTP API and envelopes

- [x] 6.1 Add validated DTO-only create, booking-number retrieval, and cancellation APIs using
  `Idempotency-Key` header and stable success/error envelopes.
  - Acceptance: first create is 201, replay is 200, validation/conflict/downstream/state failures
    use correct HTTP statuses and codes, and controllers neither accept nor return Entities.
  - Test: MockMvc/web tests for headers, bodies, statuses, trace IDs, and leakage prevention.
- [x] 6.2 Add architecture and configuration boundary checks.
  - Acceptance: Booking never depends on Resource/User implementation modules or persistence,
    controllers do not use mappers/entities, skeleton makes no DB/HTTP connection, and forbidden
    dependencies remain rejected.
  - Test: architecture tests, configuration boundary test, and dependency enforcement.

## 7. Integration evidence and handoff

- [x] 7.1 Add opt-in Booking MySQL and HTTP-stub integration suites.
  - Acceptance: fresh V001 migration, scoped uniqueness, claim/finalization, one persisted
    reservation, one effective allocation, lookup-after-timeout, compensation, and cancellation
    are proven; default verify does not start Docker.
  - Test: Booking `mysql-it`/HTTP-stub profile commands documented with results.
- [x] 7.2 Update Booking/root README, `.env.example`, runbook, and `.agent/HANDOFF.md`.
  - Acceptance: documents describe C10 variables, explicit profiles, current branch/change,
    limitations, timeout inspection, compensation procedure, exact commands, and never include
    usable secrets or local absolute paths.
  - Test: tracked-secret/path scan and documentation command review.
- [x] 7.3 Run final verification and scope review.
  - Acceptance: Booking and Resource module verification, root `clean verify`, required opt-in
    suites, strict OpenSpec validation, `git diff --check`, migration immutability review, and
    service-boundary review all pass with recorded evidence.
  - Test: the exact commands above; no task is checked without its evidence.
