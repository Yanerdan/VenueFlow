## Context

C10 creates a reservation as `CONFIRMED` immediately after Resource allocation. C11 publishes
that confirmation atomically, C13 consumes it, and C14 reconciles uncertain allocation/release
work. The full lifecycle requires a server-owned confirmation deadline, explicit simulated
confirmation, and timeout release, but Resource has no separate hold-confirm API and must remain
unchanged.

C15 therefore treats the existing Resource allocation as the capacity hold. Booking alone owns
whether that hold is pending, confirmed, cancelled, or expired. Expiration must race safely with
confirmation and cancellation, survive process stops, and preserve existing Outbox and
Notification reliability.

## Goals / Non-Goals

**Goals:**

- Create reservations as `PENDING_CONFIRMATION` with a bounded server-owned `expireAt`.
- Add idempotent explicit confirmation and pending cancellation.
- Lease expired reservations in bounded pages and release capacity before committing `EXPIRED`.
- Make confirmation, cancellation, and expiration mutually exclusive through database conditions.
- Publish confirmation only after confirmation and publish expiration through the existing Outbox.
- Extend Notification's existing reliable consumer to the expiration event.
- Preserve deterministic recovery, default Docker-free verification, and service ownership.

**Non-Goals:**

- Real payment, payment callbacks, refunds, pricing, or financial settlement.
- RabbitMQ delayed-message plugins, Redis timers/locks, Quartz, or distributed transactions.
- Resource schema/API changes, new Resource operation types, or cross-service database access.
- Authentication/Gateway work, production Compose application containers, email, search, check-in,
  completion, or historical conversion of existing confirmed reservations.

## Decisions

### 1. Use a Booking-owned pending lifecycle without changing Resource

Booking V004 expands reservation status to `PENDING_CONFIRMATION`, `CONFIRMED`, `CANCELLED`, and
`EXPIRED`; adds `expire_at` and bounded cancellation/expiration reason facts; and creates the
indexes and status audit needed for due work. The existing Resource allocation represents both a
pending hold and confirmed occupancy, so confirmation is a Booking-only transaction.

Alternative: add HELD/CONFIRMED operations to Resource. Rejected because current capacity is
already safely occupied and C15 does not need another cross-service write or API.

### 2. Make creation pending and move the confirmation event

The create owner still persists allocation recovery intent before Resource, then atomically
persists a pending reservation, idempotency success, and intent resolution. It does not append a
confirmation event. `POST /api/v1/bookings/{bookingNo}/confirmation` conditionally transitions a
non-expired pending reservation to confirmed, appends one confirmation Outbox event, and records
one status log in the same transaction.

A confirmed replay returns the same reservation. Confirmation after `expireAt`, while an active
timeout lease owns the row, or after a terminal state performs no transition or Resource write.

Alternative: retain immediate confirmation and expire confirmed bookings. Rejected because that
would release capacity from a legitimately confirmed reservation and contradict the state model.

### 3. Lease due reservations directly with optimistic conditions

V004 stores timeout lease owner/expiry, attempt count, next check, last error, and version with the
reservation. An explicit `persistence,expiration` runtime claims only bounded due
`PENDING_CONFIRMATION` rows whose deadline passed, using version CAS in a short transaction.
Network calls occur after commit; expired leases are reclaimable.

Confirmation and cancellation require no live timeout lease. Once `expireAt` passes,
confirmation is permanently rejected even if release is temporarily unavailable. This prevents
late confirmation from racing a worker that has started capacity release.

Alternative: create a separate timeout-job table or delayed RabbitMQ message. Rejected for this
increment because the reservation is already the unique durable deadline fact; another table or
broker timer adds dual-write and stale-message complexity without improving recovery.

### 4. Prove release before committing expiration

Each timeout uses the reservation's existing deterministic release operation ID. The worker first
checks the Resource operation. A matching release is proof; an absent release permits one
idempotent release call for that run. Ambiguous response is resolved by lookup. Only proven
release allows one local transaction to commit `PENDING_CONFIRMATION -> EXPIRED`, append one
expiration Outbox event, record a status log, and clear the lease.

Mismatch, Resource outage, unknown response, lease loss, or persistence failure retains bounded
retry state and never marks the reservation expired without proof. The release ID is shared with
manual cancellation, so concurrent paths converge on one effective Resource release.

### 5. Keep Outbox and Notification contracts additive

The Outbox allowlist gains `BOOKING_RESERVATION_EXPIRED` with routing key
`booking.reservation.expired.v1` and status `EXPIRED`. Notification adds one exact binding and
validates/derives expiration notifications through the existing inbox transaction, manual ACK,
retry, DLQ, and replay machinery.

No expiration event is emitted at deadline detection alone; it describes only the committed
expired state after release proof. Existing confirmed/cancelled event schema remains compatible.

### 6. Separate automatic execution and guarded operator control

The expiration scheduler exists only under `persistence,expiration` and scans only when
`VENUEFLOW_EXPIRATION_ENABLED=true`. A non-HTTP command defaults to no action, supports
metadata-only `PREVIEW`, and requires a bounded reason plus confirmation for one bounded `RUN`.
Batch, lease, scan delay, attempts, backoff, deadline duration, lookup, and HTTP timeouts are
validated.

Metrics/logs expose due depth/age, claim, confirm, expired, cancelled, retry, mismatch,
lease-reclaimed, action outcome, and shutdown without payloads or endpoints.

### 7. Verify races at database and HTTP boundaries

Default tests cover lifecycle rules, deadline validation, CAS outcomes, backoff, command guards,
event allowlists, Notification parsing, and profile isolation without Docker. `expiration-it`
uses MySQL 8.4.10 plus a bounded HTTP stub to prove V004, create-before-deadline, competing
confirmation/expiration, lease reclaim, lost release response, and one expiration Outbox event.
Existing `outbox-it` and `consumer-it` prove the new routing key with RabbitMQ 4.1.8.

## Risks / Trade-offs

- [Creation response changes status] → Mark the API change breaking, update DTO/contract tests, and
  deploy consumers that tolerate the new status before enabling expiration.
- [Existing confirmed rows have no deadline] → V004 leaves them confirmed with no timeout work;
  C15 applies only to newly created pending reservations.
- [Release succeeds before local expiration commit] → Lease reclaim and deterministic operation
  lookup complete the same transition later.
- [Confirmation races timeout claim] → Deadline and live-lease predicates allow one winner; losers
  reread and return stable state/conflict results.
- [Resource remains unavailable past the deadline] → Booking remains pending but unconfirmable,
  exposes overdue work, and retries without inventing release success.
- [New event is temporarily unroutable] → The Outbox remains retryable/dead and does not roll back
  the already committed expiration; Notification topology must be deployed before enablement.

## Migration Plan

1. Deploy Notification support and its third binding while no expiration event exists.
2. Deploy Booking V004 and the new binary with expiration disabled.
3. Verify existing confirmed rows remain unchanged and new creation/confirmation contracts work.
4. Run operator preview, then enable one expiration worker with a small batch.
5. Monitor overdue age, release outcomes, Outbox routing, and Notification deduplication.

Rollback disables expiration first and waits for active leases. V004 remains immutable. Rolling
back the API after clients have received pending reservations is unsafe; use forward recovery with
the C15 binary until all pending rows are terminal, then evaluate application rollback.

## Open Questions

None. The confirmation is deliberately simulated and unauthenticated at this milestone; real
payment and security remain later changes.
