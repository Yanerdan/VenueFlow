## ADDED Requirements

### Requirement: Effective reservation state changes append one Outbox event atomically

Booking Service SHALL append one immutable Outbox event in the same local transaction that
first persists a `CONFIRMED` reservation or wins the conditional transition to `CANCELLED`.
The event MUST describe the committed state. HTTP replay, a losing concurrent cancellation, or a
failed local transaction MUST NOT append another effective business event.

No RabbitMQ call, collaborator HTTP call, confirm wait, retry delay, or other network operation
MAY occur inside the reservation transaction. Failure to append the required Outbox event MUST
roll back the associated Booking transaction and follow the established compensation/state
handling.

#### Scenario: Reservation confirmation commits

- **WHEN** Booking atomically persists a new confirmed reservation
- **THEN** the same transaction persists one confirmation Outbox event
- **AND** idempotent create replay inserts no additional confirmation event

#### Scenario: Reservation finalization rolls back

- **WHEN** reservation, idempotency finalization, or Outbox insertion fails
- **THEN** none of those local facts commit
- **AND** the established deterministic Resource compensation path is used

#### Scenario: Cancellation transition wins

- **WHEN** one caller wins the conditional `CONFIRMED -> CANCELLED` transition
- **THEN** the same transaction persists one cancellation Outbox event
- **AND** concurrent or replayed cancellation inserts no additional cancellation event
