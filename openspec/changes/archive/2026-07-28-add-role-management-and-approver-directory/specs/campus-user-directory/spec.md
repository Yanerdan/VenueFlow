## MODIFIED Requirements

### Requirement: Authorized staff browse a bounded user directory

User Service SHALL expose a newest-first bounded profile directory to `APPROVER`, `RESOURCE_MANAGER`, and `SYSTEM_ADMIN` role contexts. The directory MUST support optional bounded keyword matching over display name, campus ID, and department, with zero-based pages and a maximum page size of 100.

#### Scenario: An approver searches a department

- **WHEN** an approver submits a bounded department keyword
- **THEN** User Service returns only the bounded matching profile page

#### Scenario: A resource manager loads assignment candidates

- **WHEN** a resource manager requests the bounded directory
- **THEN** User Service returns profiles with their stable external user IDs for responsibility assignment

#### Scenario: An applicant requests the directory

- **WHEN** an applicant requests the management directory
- **THEN** User Service returns a stable forbidden response
