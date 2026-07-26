## Why

C14 completed durable Booking/Resource reconciliation, but every successful create still becomes
`CONFIRMED` immediately, so the required confirmation deadline and timeout-release workflow do not
exist. C15 completes the remaining v0.5 reliability capability with a durable, race-safe
pending-confirmation lifecycle.

## What Changes

- **BREAKING**: successful creation returns `PENDING_CONFIRMATION` instead of `CONFIRMED` and
  includes a bounded server-owned expiration time.
- Add an idempotent confirmation operation that conditionally commits
  `PENDING_CONFIRMATION -> CONFIRMED` and appends the existing confirmation Outbox event.
- Add opt-in, leased timeout processing that releases capacity through deterministic Resource
  operations before atomically committing `PENDING_CONFIRMATION -> EXPIRED`.
- Add an expiration Outbox event and extend Notification's reliable consumer with an idempotent
  in-app expiration notification.
- Define explicit confirmation/expiration race semantics, durable retry/recovery, operator
  preview/run controls, metrics, and MySQL/RabbitMQ verification.
- Keep default verification Docker-free and exclude payment, delayed-message plugins, Redis,
  Gateway/security, production Compose scheduling, and new Resource APIs or schema.

## Capabilities

### New Capabilities

- `booking-timeout-expiration`: Pending confirmation, confirmation deadlines, leased timeout
  execution, safe capacity release, and confirmation/expiration race handling.

### Modified Capabilities

- `booking-reservation-management`: Creation becomes pending and gains an explicit idempotent
  confirmation transition.
- `booking-capacity-reconciliation`: A matching pending reservation is a consistent allocation,
  and expiration recovery remains isolated from orphan-allocation repair.
- `reliable-booking-event-publication`: Confirmation moves to the confirmation transaction and a
  new expiration event is published reliably.
- `reliable-notification-consumption`: Notification accepts the expiration event with the existing
  inbox, retry, and dead-letter guarantees.

## Impact

Booking API/status DTOs, Booking V004, reservation orchestration, Outbox event contracts,
Notification routing/validation/content, opt-in timeout execution, tests, environment examples,
and runbooks are affected. Resource remains unchanged and is accessed only through its existing
deterministic allocation, release, and operation-lookup DTO APIs.
