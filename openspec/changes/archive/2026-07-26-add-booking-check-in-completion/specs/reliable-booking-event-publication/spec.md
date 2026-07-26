## MODIFIED Requirements

### Requirement: Booking events use a stable bounded envelope

Every published event MUST use UTF-8 JSON containing event ID, event type, positive schema
version, UTC occurrence time, producer, aggregate type and identifier, optional bounded trace
ID, and a typed bounded payload. Booking SHALL publish
`booking.reservation.confirmed.v1`, `booking.reservation.cancelled.v1`,
`booking.reservation.expired.v1`, and `booking.reservation.completed.v1` routing keys with
payload status matching the effective committed Booking state.

Events MUST NOT contain credentials, connection strings, raw collaborator bodies, stack traces,
large binary content, or internal database implementation details. Payload and header bytes MUST
be rejected before persistence when they exceed configured/schema limits.

#### Scenario: A confirmed reservation event is serialized

- **WHEN** Booking confirms a pending reservation and creates its Outbox event
- **THEN** its envelope has the stable event ID, versioned type, UTC time, booking number, user,
  slot, quantity, and `CONFIRMED` status
- **AND** it contains no secret or infrastructure configuration

#### Scenario: An expired reservation event is serialized

- **WHEN** Booking commits a proven timeout expiration
- **THEN** the envelope uses the expiration routing key and `EXPIRED` status
- **AND** it contains only the existing bounded reservation identity and quantity facts

#### Scenario: A completed reservation event is serialized

- **WHEN** Booking commits an eligible check-in
- **THEN** the envelope uses the completion routing key and `COMPLETED` status
- **AND** it contains only the existing bounded reservation identity and quantity facts

#### Scenario: An event exceeds the size limit

- **WHEN** a generated envelope exceeds the configured payload or header limit
- **THEN** the local business transaction fails before an oversized event is committed
