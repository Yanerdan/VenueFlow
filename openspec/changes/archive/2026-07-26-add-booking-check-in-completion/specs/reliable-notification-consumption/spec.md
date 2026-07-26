## MODIFIED Requirements

### Requirement: Booking event envelopes are validated before side effects

The consumer SHALL accept only UTF-8 JSON matching the bounded Booking envelope and the exact
`booking.reservation.confirmed.v1`, `booking.reservation.cancelled.v1`,
`booking.reservation.expired.v1`, or `booking.reservation.completed.v1` routing contract. It
MUST validate event ID, type/routing agreement, version, producer, aggregate identity, UTC
occurrence time, typed payload, status, content type, and configured byte limits before creating
a notification.

Unknown versions/types, malformed JSON, mismatched routing or payload status, missing required
facts, oversized content, and event-ID reuse with a different canonical hash MUST be terminal
failures and MUST NOT create a notification.

#### Scenario: A valid confirmation event arrives

- **WHEN** a confirmation envelope and routing key agree and pass all bounds
- **THEN** the consumer derives a confirmation notification from typed fields
- **AND** does not persist or log the raw envelope

#### Scenario: A valid expiration event arrives

- **WHEN** an expiration envelope has the exact routing key and `EXPIRED` payload status
- **THEN** the consumer derives one expiration notification through the existing inbox transaction
- **AND** duplicate delivery creates no second notification

#### Scenario: A valid completion event arrives

- **WHEN** a completion envelope has the exact routing key and `COMPLETED` payload status
- **THEN** the consumer derives one completion notification through the existing inbox transaction
- **AND** duplicate delivery creates no second notification

#### Scenario: An event identity is reused with different content

- **WHEN** an event ID already consumed by this consumer arrives with a different type, version,
  or canonical payload hash
- **THEN** the message is classified as an identity collision
- **AND** no existing consumed event or notification is changed

### Requirement: Consumer topology is durable and consumer-owned

Only the explicit `persistence,messaging` runtime SHALL create a Notification-owned durable work
queue, fixed-backoff retry queue, dead-letter exchange and queue, and exact bindings for the four
Booking routing keys on the existing durable `venueflow.events.v1` topic exchange. Messages and
republished retry/dead-letter messages MUST be persistent.

Queue names, prefetch, concurrency, retry delay, maximum attempts, message byte limit, and
publish-confirm timeout MUST be validated and bounded. Booking Service MUST NOT own or declare
Notification queues, and Notification MUST NOT modify the exchange ownership or event payload
contract.

#### Scenario: Messaging is explicitly enabled

- **WHEN** Notification starts with valid `persistence,messaging` database and RabbitMQ settings
- **THEN** its durable work, retry, and dead-letter topology is declared idempotently
- **AND** confirmation, cancellation, expiration, and completion routing keys are routable

#### Scenario: Default skeleton starts

- **WHEN** Notification starts without explicit persistence and messaging profiles
- **THEN** no datasource, RabbitMQ connection, listener container, queue, or binding is created
