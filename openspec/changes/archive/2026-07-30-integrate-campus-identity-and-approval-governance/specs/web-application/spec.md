## ADDED Requirements

### Requirement: Login presents configured campus identity providers
The login workspace SHALL load enabled provider readiness from Auth, SHALL start sign-in only for a ready provider, and SHALL keep local demonstration login visibly distinct from campus identity sign-in.

#### Scenario: Campus provider is ready
- **WHEN** a visitor selects the provider
- **THEN** the browser begins the server-issued authorization flow without receiving a client secret

### Requirement: Workspaces display authoritative organization identity
Applicant and management workspaces SHALL distinguish directory-synchronized campus and organization facts from user-maintained contact facts and SHALL display the last synchronization state where relevant.

#### Scenario: Directory-bound profile is displayed
- **WHEN** the authenticated profile contains active authoritative membership
- **THEN** the workspace identifies the organization as synchronized and does not present it as freely editable

### Requirement: Workspaces render arbitrary bounded approval progress
Applicant and management workspaces SHALL render the ordered labels, assignees, current position, and completed actions for approval policies of one to five stages.

#### Scenario: A booking is at stage three of four
- **WHEN** the booking detail is opened
- **THEN** the workspace shows stages one and two completed, stage three pending, and stage four upcoming
