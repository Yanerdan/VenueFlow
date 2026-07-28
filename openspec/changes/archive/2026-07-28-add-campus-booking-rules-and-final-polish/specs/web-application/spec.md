## ADDED Requirements

### Requirement: Web application presents booking rules and role-appropriate management

The web application SHALL display resource booking rules to applicants, provide rule editing to authorized resource management roles, and hide management sections that are not applicable to the signed-in role.

#### Scenario: Applicant prepares a reservation

- **WHEN** an applicant views a resource or starts a reservation
- **THEN** the current booking notice and time limits are visible

#### Scenario: Management user enters the workspace

- **WHEN** a resource manager, approver, or system administrator enters management
- **THEN** navigation and management actions are limited to sections applicable to that role
