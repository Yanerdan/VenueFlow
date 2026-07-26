## MODIFIED Requirements

### Requirement: Booking APIs use bounded DTOs and stable envelopes

Booking Service SHALL provide DTO-only create, booking-number retrieval, confirmation,
cancellation, and check-in APIs. Controllers MUST NOT accept or return persistence Entities or
invoke Mappers. Successful responses MUST use `code`, `message`, `data`, and `traceId`; failures
MUST use only `code`, `message`, `details`, `traceId`, and `timestamp`. DTO status MUST support
`PENDING_CONFIRMATION`, `CONFIRMED`, `CANCELLED`, `EXPIRED`, and `COMPLETED`; pending responses
MUST expose the bounded server-owned expiration time and completed responses MUST expose the UTC
completion time.

Validation, idempotency conflict, in-progress, not found, eligibility, capacity, downstream,
unknown outcome, persistence, compensation, deadline, timeout ownership, check-in window, and
state failures MUST have distinct stable codes and appropriate non-200 HTTP statuses.

#### Scenario: A caller retrieves a reservation safely

- **WHEN** an existing booking number is requested
- **THEN** Booking returns a bounded reservation DTO in the success envelope
- **AND** no Entity, SQL, collaborator body, or secret is exposed

#### Scenario: A pending reservation is returned

- **WHEN** creation or retrieval returns `PENDING_CONFIRMATION`
- **THEN** the DTO includes its exact server-owned expiration time
- **AND** it exposes no timeout lease or internal retry fact

#### Scenario: A completed reservation is returned

- **WHEN** check-in or retrieval returns `COMPLETED`
- **THEN** the DTO includes its exact UTC completion time
- **AND** it exposes no collaborator response or internal audit fact

### Requirement: Effective reservation state changes append one Outbox event atomically

Booking Service SHALL append one immutable Outbox event in the same local transaction that wins
the effective transition to `CONFIRMED`, `CANCELLED`, `EXPIRED`, or `COMPLETED`. Pending
reservation creation MUST append no state event. The event MUST describe the committed state.
HTTP replay, a losing concurrent confirmation/cancellation/expiration/completion, or a failed
local transaction MUST NOT append another effective business event.

No RabbitMQ call, collaborator HTTP call, confirm wait, retry delay, or other network operation
MAY occur inside the reservation transaction. Failure to append the required Outbox event MUST
roll back the associated Booking transition and follow the established recovery handling.

#### Scenario: Reservation confirmation commits

- **WHEN** Booking atomically transitions a pending reservation to confirmed
- **THEN** the same transaction persists one confirmation Outbox event
- **AND** idempotent confirmation replay inserts no additional event

#### Scenario: Reservation finalization rolls back

- **WHEN** reservation transition or Outbox insertion fails
- **THEN** none of those local facts commit
- **AND** the established deterministic recovery path remains available

#### Scenario: Cancellation transition wins

- **WHEN** one caller wins the conditional transition to `CANCELLED`
- **THEN** the same transaction persists one cancellation Outbox event
- **AND** concurrent or replayed cancellation inserts no additional event

#### Scenario: Expiration transition wins

- **WHEN** one timeout worker wins the conditional transition to `EXPIRED`
- **THEN** the same transaction persists one expiration Outbox event
- **AND** a losing confirmation, cancellation, or timeout worker inserts no event

#### Scenario: Completion transition wins

- **WHEN** one eligible check-in wins the conditional transition to `COMPLETED`
- **THEN** the same transaction persists one completion Outbox event
- **AND** a losing cancellation or replayed check-in inserts no event
