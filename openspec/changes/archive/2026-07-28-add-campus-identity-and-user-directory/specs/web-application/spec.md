## ADDED Requirements

### Requirement: Applicant workspace manages campus identity

The applicant workspace SHALL collect bounded campus identity fields during registration and
provide a profile view where the authenticated user can review and update those facts.

#### Scenario: A new applicant completes registration

- **WHEN** registration and profile creation succeed with campus information
- **THEN** the profile workspace displays the server-owned campus identity

### Requirement: Management workspace presents a user directory

The management workspace SHALL provide a user directory with bounded keyword search and SHALL
resolve booking applicant IDs to available display name and department facts. Missing optional
facts MUST be shown as incomplete rather than invented.

#### Scenario: An approver views booking applications

- **WHEN** the management workspace has matching User and Booking records
- **THEN** each booking row shows the applicant display name and department
