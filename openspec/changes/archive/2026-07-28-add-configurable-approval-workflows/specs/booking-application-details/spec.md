## MODIFIED Requirements

### Requirement: Booking stores complete application details

Booking Service SHALL persist bounded application title, purpose, contact name, contact phone, attendee quantity, optional note, resource responsibility snapshot, approval-chain snapshot, and ordered approval actions with each reservation while preserving the existing booking state machine and idempotency behavior.

#### Scenario: Applicant submits complete application details
- **WHEN** an authenticated applicant creates a booking with valid required fields
- **THEN** Booking persists and returns the application, responsibility, and approval stage, while its approval-action API returns an empty history

#### Scenario: Applicant omits a required detail
- **WHEN** the application title, purpose, contact name, or contact phone is blank
- **THEN** Booking returns a bounded invalid-request response without creating a reservation
