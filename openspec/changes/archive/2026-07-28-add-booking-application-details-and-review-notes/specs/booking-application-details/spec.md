## ADDED Requirements

### Requirement: Booking owns bounded application details

Booking Service SHALL own a bounded activity title, purpose, contact name, contact phone, and
optional note for each new reservation application. Required fields MUST be nonblank and all
fields MUST be trimmed and length-bounded. Existing reservations without these additive fields
MUST remain readable without invented details.

#### Scenario: An applicant submits complete application details

- **WHEN** a valid reservation request includes all required application fields
- **THEN** Booking persists and returns those details with the reservation

#### Scenario: A historical reservation is read

- **WHEN** a reservation created before the additive migration is retrieved
- **THEN** Booking returns it successfully with absent optional application details

### Requirement: Booking records a bounded review conclusion

Booking Service SHALL allow an authorized approver to confirm or reject a pending application
with a bounded review note. Rejection MUST require a nonblank reason, SHALL transition the
reservation to `CANCELLED`, and SHALL expose the rejection conclusion, reviewer role, and review
time in the bounded reservation DTO.

#### Scenario: An approver confirms with a note

- **WHEN** an authorized approver confirms a pending application with a bounded note
- **THEN** Booking returns `CONFIRMED` with the persisted review conclusion and time

#### Scenario: An approver rejects without a reason

- **WHEN** an authorized approver attempts rejection without a nonblank reason
- **THEN** Booking returns a validation failure and preserves the pending state

### Requirement: Application details participate in idempotency

Booking Service MUST include normalized application detail fields in the create request hash.
Reusing one scoped idempotency key with different application details MUST return the established
idempotency conflict without another capacity allocation.

#### Scenario: A note changes under the same idempotency key

- **WHEN** a caller reuses a successful create key with a different application note
- **THEN** Booking returns an idempotency conflict and performs no collaborator write
