## ADDED Requirements

### Requirement: Scoped operational summary
The system SHALL provide authenticated campus approval roles with a read-only operational summary derived from persisted bookings, and SHALL restrict an `APPROVER` summary to bookings assigned to that trusted user while allowing a `SYSTEM_ADMIN` to view the global summary.

#### Scenario: System administrator views global metrics
- **WHEN** a system administrator requests the operational report
- **THEN** the system returns totals for all persisted bookings, pending bookings, approved bookings, completed bookings, total requested attendees, and approval rate

#### Scenario: Approver views assigned metrics
- **WHEN** an approver requests the operational report with a trusted user identity
- **THEN** every returned metric and breakdown includes only bookings assigned to that approver

#### Scenario: Applicant is denied
- **WHEN** an applicant requests the operational report
- **THEN** the system rejects the request as forbidden

### Requirement: Resource and department breakdowns
The system SHALL aggregate booking count and requested attendee count by resource snapshot and owner-department snapshot.

#### Scenario: Management compares resources
- **WHEN** the operational report contains bookings for multiple resources
- **THEN** the report returns resource rows ordered by booking count and attendee count with stable resource identifiers

#### Scenario: Booking has no department snapshot
- **WHEN** a historical booking has no owner-department snapshot
- **THEN** the booking is included in an explicit unassigned department row

### Requirement: Recent approval audit
The system SHALL return the most recent persisted approval decisions with booking number, decision, reviewer role, review note, and review time.

#### Scenario: Recent decisions are displayed
- **WHEN** management requests the operational report
- **THEN** up to twenty reviewed bookings are returned in reverse chronological review order

### Requirement: Administrative reporting workspace
The management web application SHALL present the operational summary, resource ranking, department distribution, and recent approval audit in a dedicated reporting workspace.

#### Scenario: Administrator opens reporting
- **WHEN** an authorized administrator opens the reporting workspace
- **THEN** the page displays server-derived metrics and breakdowns with clear empty states and without requiring manual calculation
