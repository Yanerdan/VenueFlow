## Why

C15 completes pending confirmation and timeout recovery, but VenueFlow still cannot finish the
documented user journey because a confirmed reservation cannot be checked in and marked
`COMPLETED`. C16 closes that lifecycle gap before the project moves into Gateway/authentication
and cache/search infrastructure.

## What Changes

- Add a bounded, state-idempotent check-in API that transitions an eligible `CONFIRMED`
  reservation to `COMPLETED`.
- Read the existing Resource slot time range and allow check-in only inside a configurable,
  server-owned window; collaborator failure or invalid time leaves Booking unchanged.
- Persist the completion timestamp and status audit through a new immutable Booking migration.
- Append one `booking.reservation.completed.v1` Outbox event in the same transaction as the
  winning transition.
- Extend Notification's existing inbox consumer to create one in-app completion notification.
- Keep default verification Docker-free and add focused opt-in MySQL/HTTP and consumer evidence.
- Defer QR codes, authentication/authorization, Gateway, payment, reviews, Redis, search, and
  production Compose application scheduling.

## Capabilities

### New Capabilities

- `booking-check-in-completion`: Check-in eligibility, Resource slot-time lookup, atomic
  completion, idempotency, auditing, and verification boundaries.

### Modified Capabilities

- `booking-reservation-management`: Add the `COMPLETED` terminal state and bounded check-in API.
- `reliable-booking-event-publication`: Add the completion event to the existing bounded Outbox
  envelope and routing contract.
- `reliable-notification-consumption`: Accept completion events through the existing durable,
  idempotent Notification consumer.

## Impact

- Booking Service receives an additive V005 migration, a Resource slot read adapter, completion
  orchestration, one endpoint, one Outbox event type, and focused tests/documentation.
- Notification Service receives an additive migration only if its current type constraint
  requires it, plus decoder, routing binding, deterministic content, and consumer tests.
- Resource Service schema and APIs remain unchanged; Booking uses its existing bounded slot read.
- No new infrastructure, runtime service, cross-schema access, or external dependency is added.
