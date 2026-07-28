## ADDED Requirements

### Requirement: Management booking history is bounded and role-protected

Booking Service SHALL expose a newest-first global booking query with optional status filtering.
The query MUST use zero-based pages, default page size 20, maximum page size 100, bounded DTOs,
and MUST reject callers without `APPROVER` or `SYSTEM_ADMIN` role context.

#### Scenario: An approver requests pending applications

- **WHEN** an approver requests the global booking page filtered by `PENDING_CONFIRMATION`
- **THEN** Booking returns a bounded newest-first page containing matching reservations

#### Scenario: An applicant requests global history

- **WHEN** an applicant calls the management booking query
- **THEN** Booking returns a stable forbidden response without executing an unbounded query

## MODIFIED Requirements

### Requirement: Booking APIs use bounded DTOs and stable envelopes

Booking Service SHALL provide DTO-only create, booking-number retrieval, management confirmation,
cancellation, management check-in, user history, and management history APIs. Confirmation and
check-in MUST require an `APPROVER` or `SYSTEM_ADMIN` trusted role header. Controllers MUST NOT
accept or return persistence Entities or invoke Mappers. Successful responses MUST use `code`,
`message`, `data`, and `traceId`; failures MUST use only `code`, `message`, `details`, `traceId`,
and `timestamp`. DTO status MUST support `PENDING_CONFIRMATION`, `CONFIRMED`, `CANCELLED`,
`EXPIRED`, and `COMPLETED`; pending responses MUST expose the bounded server-owned expiration
time and completed responses MUST expose the UTC completion time.

Validation, forbidden, idempotency conflict, in-progress, not found, eligibility, capacity,
downstream, unknown outcome, persistence, compensation, deadline, timeout ownership, check-in
window, and state failures MUST have distinct stable codes and appropriate non-200 HTTP statuses.

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

#### Scenario: An applicant attempts an approval action

- **WHEN** an applicant calls confirmation or check-in
- **THEN** Booking returns a stable forbidden response
- **AND** the reservation state is unchanged
