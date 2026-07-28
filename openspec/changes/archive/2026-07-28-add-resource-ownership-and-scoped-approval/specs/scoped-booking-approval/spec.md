## ADDED Requirements

### Requirement: Booking snapshots resource responsibility

Booking Service SHALL snapshot resource ID, owning department, and assigned approver external user
ID for a new reservation from Resource-owned slot facts. Historical bookings without those facts
MUST remain readable.

#### Scenario: A reservation is created for an assigned resource

- **WHEN** Resource returns responsibility for the selected slot
- **THEN** Booking persists and returns the same assignment with the reservation

### Requirement: Approval access is assignment-scoped

Booking Service MUST restrict `APPROVER` management queries, confirmation, rejection, and check-in
to reservations assigned to the trusted caller external user ID. `SYSTEM_ADMIN` SHALL retain
global access. A forbidden action MUST preserve reservation state.

#### Scenario: An assigned approver loads pending work

- **WHEN** an approver requests the management queue with trusted identity
- **THEN** Booking returns only reservations assigned to that identity

#### Scenario: Another approver attempts approval

- **WHEN** an approver acts on a reservation assigned to someone else
- **THEN** Booking returns the established forbidden response and preserves its state

#### Scenario: A system administrator loads pending work

- **WHEN** a system administrator requests the management queue
- **THEN** Booking returns the bounded global result
