## Context

Resource Service owns `resource_slot`, occupied quantity, and the V003 allocation-operation
ledger. User Service owns profile and booking-eligibility facts. Booking Service currently has
only an infrastructure-free skeleton. C10 must add the first reservation flow while respecting
those ownership boundaries.

The repository already contains an uncommitted partial C10 implementation. It is evidence of an
attempt, not a source of truth: the engineering specification, main specs, and this corrected
change remain authoritative.

## Goals

- Persist Booking-owned idempotency and reservation facts in an explicit Booking schema.
- Ensure only one concurrent executor can call Resource for one scoped create request.
- Resolve an HTTP timeout after an allocation write without guessing whether Resource changed.
- Provide DTO-only create, retrieve, and cancellation APIs with stable envelopes.
- Keep default startup and verification independent of MySQL and collaborator services.
- Produce test evidence for replay, concurrency, rejection, timeout, compensation, and
  cancellation boundaries.

## Non-Goals

Auth, Gateway, Feign/Nacos, MQ/Outbox, Redis, search, payment, expiry, check-in/completion,
distributed transactions, shared entities, cross-service database access, and durable
background reconciliation are not introduced.

## Existing Contract Findings

- User exposes `GET /api/v1/users/{userId}/booking-eligibility` and returns a bounded
  `bookingPermitted` fact.
- Resource exposes allocation and release writes using `{operationId, quantity}` and enforces
  operation-ID idempotency in its database.
- Resource exposes bounded operation pages but no direct lookup by operation ID. A direct
  Resource-owned lookup is therefore added so Booking can resolve ambiguous write outcomes
  without paging or reading the Resource database.

## Options Considered

| Option | Advantages | Rejected limitation |
|---|---|---|
| Query then insert one reservation row | Small implementation | Concurrent requests can both call Resource; a losing insert can compensate the winner |
| Hold a Booking DB transaction open across User/Resource HTTP calls | Simple ownership lock | Long network calls inside transactions violate the engineering baseline and exhaust DB resources |
| Atomic idempotency claim, remote calls outside the transaction, final local transaction | One executor, short transactions, explicit recovery states | Requires a separate idempotency fact and state handling |
| Add Feign/Nacos now | Familiar service client model | Prematurely introduces v0.3 infrastructure and changes skeleton startup |

Decision: use a separate Booking idempotency record, short local transactions, deterministic
Resource operation IDs, and Java HTTP adapters.

## Data Model

### `booking_idempotency`

```text
id
user_id
operation                 CREATE
idempotency_key
request_hash
request_id                UUID correlation owned by Booking
booking_id                nullable until success
status                    PROCESSING | SUCCEEDED | FAILED
failure_code              nullable
created_at
updated_at
expires_at                nullable; no cleanup job in C10
version
```

Constraints:

- unique `(user_id, operation, idempotency_key)`;
- unique `request_id`;
- bounded key and hash lengths;
- status and operation checks;
- `booking_id` references only the Booking-owned reservation table.

### `booking_reservation`

```text
id
booking_no                externally safe UUID/ULID-style value
request_id                unique correlation to idempotency
user_id
slot_id
quantity
status                    CONFIRMED | CANCELLED
allocation_operation_id   unique deterministic Resource operation ID
release_operation_id      unique deterministic Resource operation ID
version
created_at
confirmed_at
cancelled_at              nullable
updated_at
```

The C10 reservation is synchronously confirmed after Resource allocation. This intentionally
uses the engineering state-machine state `CONFIRMED`; it does not introduce the incompatible
temporary state `ACTIVE`. `PENDING_CONFIRMATION`, `COMPLETED`, and `EXPIRED` are deferred until
their workflows exist.

## API Decisions

### Create

```http
POST /api/v1/bookings
Idempotency-Key: <UUID>
Content-Type: application/json

{"userId":1,"slotId":2,"quantity":1}
```

- The idempotency key is never duplicated in the body.
- A normalized hash covers `userId`, `slotId`, and `quantity`.
- Success returns HTTP 201 for the first completed request and HTTP 200 for a replay, both with
  the same success-envelope data.
- A matching request still `PROCESSING` returns a stable bounded in-progress response; it does
  not become a second executor.
- Same scoped key with another hash returns HTTP 409 before User/Resource calls.

### Retrieve

`GET /api/v1/bookings/{bookingNo}` returns one bounded Booking DTO. Internal Entity classes are
never used by the controller contract.

### Cancel

`POST /api/v1/bookings/{bookingNo}/cancellation` is state-idempotent in C10:

- the first request releases the stored quantity using the reservation's deterministic release
  operation ID and then performs `CONFIRMED -> CANCELLED`;
- replay of an already cancelled reservation returns the same cancelled result;
- no separate cancellation key is required because only one terminal cancellation fact exists
  for a booking and Resource release is independently idempotent;
- this assessment is recorded explicitly to satisfy the external-write idempotency rule.

## Create Sequence and Transaction Boundaries

```text
Client        Booking DB          User             Resource
  | POST+key      |                 |                  |
  |-------------->| claim TX       |                  |
  |                | INSERT/READ    |                  |
  |                |--commit------->|                  |
  |                |                 | GET eligibility  |
  |                |---------------------------------->|
  |                |<----------------------------------|
  |                |                 | POST allocation  |
  |                |------------------------------------>|
  |                |<------------------------------------|
  |                | final TX: reservation + idempotency SUCCEEDED
  |<---------------|                 |                  |
```

Claim and finalization are separate short local transactions. No database transaction remains
open during HTTP calls.

Claim outcomes:

1. New key: insert `PROCESSING`; caller becomes the only executor.
2. Matching `SUCCEEDED`: return existing reservation.
3. Matching `PROCESSING`: return in-progress; do not call collaborators.
4. Matching `FAILED`: return the recorded safe failure in C10; retry policy is deferred and the
   client must use a new key.
5. Different hash: return conflict; do not call collaborators.

The unique constraint is authoritative. A concurrent insert loser reloads and follows the same
outcome table.

## Allocation Timeout Resolution

Allocation and release writes have connect and request timeouts and are never automatically
retried.

If allocation returns no definitive HTTP response:

1. Query `GET /api/v1/resource-slots/{slotId}/allocation-operations/{operationId}`.
2. If it returns a matching `ALLOCATE` operation and fingerprint facts, treat allocation as
   successful.
3. If Resource definitively reports not found after a small bounded lookup window, record a
   retryable downstream failure and create no reservation.
4. If the outcome remains unknown, record `BOOKING_ALLOCATION_OUTCOME_UNKNOWN`, create no
   reservation, and expose the operation ID only in structured internal logs/runbook output.
5. Never issue a second allocation write for the same attempt.

The Resource lookup response contains operation ID, type, quantity, slot capacity snapshot, and
timestamps only. It exposes no Entity or SQL detail.

## Local Persistence Failure and Compensation

After a confirmed allocation, the final Booking transaction inserts `booking_reservation` and
updates idempotency to `SUCCEEDED`.

If this transaction fails:

1. attempt one Resource release with deterministic ID
   `release:<allocationOperationId>`;
2. if release succeeds or Resource reports the identical release already exists, record a safe
   failed outcome where possible and return `BOOKING_PERSISTENCE_FAILED`;
3. if release fails or remains ambiguous, return `BOOKING_COMPENSATION_REQUIRED`, emit a
   correlation-rich error log, and follow the manual inspection runbook;
4. do not claim that durable recovery exists in C10.

## Cancellation Sequence and Concurrency

```text
load CONFIRMED reservation
  -> Resource release using stored deterministic release ID
  -> short Booking TX with WHERE status='CONFIRMED' AND version=:expected
  -> if update count is 0, reload
       CANCELLED => idempotent success
       otherwise => stable state/version conflict
```

Concurrent cancellations can send the same release operation ID, which Resource resolves
idempotently. Only one local conditional update succeeds.

## Failure Matrix

| Failure | Local state | Remote action | Client result |
|---|---|---|---|
| Invalid key/body | none | none | validation error |
| Key/hash conflict | existing unchanged | none | 409 idempotency conflict |
| Matching in progress | `PROCESSING` | none | bounded in-progress error |
| User denied | idempotency `FAILED` | none | booking not eligible |
| User unavailable | idempotency `FAILED` | none | downstream unavailable |
| Resource rejects capacity | idempotency `FAILED` | no allocation | stable capacity error |
| Allocation timeout, lookup proves success | `PROCESSING` then finalize | existing allocation | normal continuation |
| Allocation outcome unknown | `FAILED` where possible | no retry | outcome-unknown error |
| Booking final TX fails, release succeeds | failed where possible | compensated | persistence failure |
| Booking final TX fails, release fails | failed/unknown | manual action required | compensation-required |
| Cancel release rejected/unavailable | reservation remains `CONFIRMED` | no local transition | safe downstream error |
| Concurrent cancel | one `CANCELLED` | one effective release | same cancelled result |

## HTTP Client Policy

- Java `HttpClient`, configured only in `persistence`.
- Environment-only base URLs.
- Bounded connect and request timeouts.
- No automatic retry for allocation or release writes.
- At most a small bounded retry for GET result lookup.
- Distinguish validation/capacity rejection, not found, collaborator unavailable, timeout, and
  malformed response.
- Never concatenate untrusted JSON; use typed request/response DTO serialization.
- Preserve interrupt status.

## Error and Success Envelopes

Business errors use only `code`, `message`, `details`, `traceId`, and `timestamp`. Codes include:

```text
BOOKING_VALIDATION_FAILED
BOOKING_IDEMPOTENCY_CONFLICT
BOOKING_REQUEST_IN_PROGRESS
BOOKING_NOT_FOUND
BOOKING_USER_NOT_ELIGIBLE
BOOKING_CAPACITY_UNAVAILABLE
BOOKING_DOWNSTREAM_UNAVAILABLE
BOOKING_ALLOCATION_OUTCOME_UNKNOWN
BOOKING_PERSISTENCE_FAILED
BOOKING_COMPENSATION_REQUIRED
BOOKING_STATE_CONFLICT
```

Success responses follow the repository `code/message/data/traceId` envelope. SQL, stack traces,
JDBC URLs, base URLs, credentials, and raw collaborator bodies are never exposed.

## Security

- Validate `Idempotency-Key` as UUID syntax with bounded length.
- Validate positive identifiers and quantity.
- Do not trust a User or Resource fact not returned by its owning API.
- Do not log credentials, complete raw bodies, or environment values.
- Authentication and authorization remain explicitly absent; local/demo callers supply `userId`
  until the security milestone.

## Observability

C10 does not introduce a metrics or tracing dependency. It does require structured application
logs containing available correlation fields: traceId, requestId, bookingNo, operationId,
outcome, and error code. Sensitive configuration is excluded. Metrics and distributed tracing
are deferred to their planned milestones.

## Verification Strategy

Default Docker-free verification:

- idempotency claim state tests;
- request-hash normalization tests;
- create replay and conflicting reuse;
- concurrent claim proves one collaborator execution;
- eligibility/capacity rejection;
- allocation timeout lookup outcomes;
- persistence failure and compensation success/failure;
- cancellation replay and concurrent conditional update;
- DTO validation, headers, envelopes, and Entity boundary;
- skeleton startup and dependency enforcement.

Opt-in verification:

- fresh MySQL applies Booking V001;
- scoped uniqueness and status constraints;
- repository claim/finalization and optimistic cancellation;
- HTTP stub verifies exact User/Resource paths, typed bodies, timeouts, response mapping, and
  operation lookup;
- combined test proves one persisted reservation and one effective allocation.

## Migration and Rollback

- Add immutable Booking `V001__init_booking_reservations.sql`.
- Do not alter Resource V001-V003 or User V001.
- Rollback stops the Booking persistence profile and API while leaving the additive schema
  intact.
- No down migration or automatic table deletion is allowed.
- A later schema correction uses Booking V002.

## Existing Partial Implementation Audit

| Existing item | Disposition | Reason |
|---|---|---|
| Booking persistence dependencies and profile | Rework then retain | Direction is valid; add timeout settings, enforce env-only values, and verify skeleton boundary |
| `V001__create_booking_reservation.sql` | Replace before release | Missing separate scoped idempotency table and uses incompatible `ACTIVE` state; unreleased migration may be replaced now |
| `BookingStatus.ACTIVE/CANCELLED` | Replace | Must align with `CONFIRMED/CANCELLED` |
| `BookingReservationEntity` and mapper | Rework | Entity may remain persistence-only; fields and encapsulation must follow corrected schema |
| `BookingReservationService` | Redesign | Query-then-call is not atomic and can compensate another concurrent winner |
| Java HTTP client direction | Retain and rework | Keep Java client, but use typed JSON, failure mapping, timeout lookup, and configurable bounded policies |
| Controller route direction | Rework | Move key to header, DTO-only boundary, stable envelopes, booking number lookup, and cancellation route |
| Existing skeleton boundary test change | Reassess | Persistence config must be allowed without weakening tracked-secret checks |
| Existing C10 tests | None to retain | New behavior currently has no test evidence |

No existing C10 implementation task is marked complete solely because a file already exists.
