## Context

Booking now owns `PENDING_CONFIRMATION`, `CONFIRMED`, `CANCELLED`, and `EXPIRED`, with atomic
Outbox publication and status audit. Resource already exposes a bounded slot read containing UTC
start/end times. Notification already consumes versioned Booking lifecycle events
idempotently. The remaining core lifecycle transition is `CONFIRMED -> COMPLETED`.

## Goals / Non-Goals

**Goals:**

- Add a minimal check-in operation with deterministic temporal eligibility.
- Make completion state-idempotent and race-safe with cancellation.
- Commit completion, audit, and one Outbox event atomically.
- Reuse the existing Resource read and Notification inbox patterns.
- Preserve Docker-free default verification.

**Non-Goals:**

- QR/barcode issuance, scanning hardware, geofencing, attendance proof, or operator UI.
- Authentication, authorization, Gateway routing, payment, refunds, reviews, and search.
- Resource schema/API changes, capacity release on completion, or a new messaging pattern.

## Decisions

1. **Use a simple booking-number check-in endpoint.** C16 adds
   `POST /api/v1/bookings/{bookingNo}/check-in`. State idempotency is sufficient because the
   operation has no external write. QR/token security is deferred until Auth and Gateway exist.
   An alternative request-id ledger would add persistence without improving the current
   transition boundary.

2. **Use Resource's existing slot read as the time source.** Booking reads the slot only for a
   currently confirmed reservation, validates the returned slot identity and UTC interval, and
   accepts check-in from `startAt - earlyWindow` through `endAt + lateWindow`, inclusively.
   Configured windows are bounded and fail fast. Copying slot times into every Booking would
   duplicate Resource-owned facts and require synchronization.

3. **Make the local transition the sole winner boundary.** V005 expands the Booking lifecycle
   constraint and adds nullable `completed_at`. A conditional status/version update commits
   `COMPLETED`, one existing status-audit row, and one immutable Outbox event in a single
   transaction. Completed replay returns the stored reservation without another Resource read,
   audit, or event. Cancellation and check-in races converge through the same conditional update.

4. **Extend existing event and consumer contracts additively.** Completion uses
   `booking.reservation.completed.v1` with the established envelope and payload. Notification
   binds that route and derives one deterministic in-app notification through its existing inbox
   transaction. A new Notification migration widens its bounded event-type constraint if needed;
   published migrations remain immutable.

5. **Keep integration evidence focused.** Default tests cover time-window boundaries, state
   decisions, replay, race outcomes, envelopes, configuration, and architecture. An opt-in
   `checkin-it` Booking suite uses MySQL 8.4.10 and a bounded HTTP stub. The existing Notification
   consumer integration profile proves durable completion routing and duplicate convergence.

## Risks / Trade-offs

- **Resource is unavailable during check-in** → Return a stable downstream error and leave the
  confirmed reservation untouched; callers can retry safely.
- **Slot time changes between booking and arrival** → Resource remains authoritative for C16;
  future policy may snapshot terms if product requirements demand it.
- **Booking number alone is not authorization** → Keep the endpoint out of production exposure
  until the planned JWT/Gateway change enforces ownership and operator roles.
- **Check-in and cancellation race** → Conditional status/version updates allow exactly one
  terminal transition and one matching event.
- **Broker delivery duplicates** → Existing Outbox/inbox identity boundaries retain
  at-least-once delivery with idempotent notification side effects.

## Migration Plan

1. Apply additive Booking V005 and Notification V003 migrations.
2. Deploy consumers with completion routing support.
3. Deploy Booking completion API/event production.
4. Roll back application binaries only; retain additive migrations and ignore unused nullable
   fields/routes on older code.

## Open Questions

None for C16. Authentication and richer attendance proof are intentionally separate changes.
