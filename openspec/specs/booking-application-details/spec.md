# Booking Application Details Specification

## Purpose

Define bounded application context and review conclusions owned by Booking Service.

## Requirements

### Requirement: Booking stores complete application details

Booking Service SHALL persist bounded application title, purpose, contact name, contact phone,
attendee quantity, optional note, resource responsibility snapshot, approval-chain snapshot, and
ordered approval actions with each reservation while preserving the existing booking state
machine and idempotency behavior.

#### Scenario: An applicant submits complete application details

- **WHEN** an authenticated applicant creates a booking with valid required fields
- **THEN** Booking persists and returns the application, responsibility, and approval stage, while its approval-action API returns an empty history

#### Scenario: Applicant omits a required detail

- **WHEN** the application title, purpose, contact name, or contact phone is blank
- **THEN** Booking returns a bounded invalid-request response without creating a reservation

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
