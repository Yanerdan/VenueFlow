# Web Application Specification

## Purpose

Define the directly runnable browser application and its bounded user journey through Gateway.

## Requirements

### Requirement: Web application is dependency-free and directly runnable

The repository SHALL provide `venueflow-web` as browser-native HTML, CSS, and ES modules with no
runtime package dependency or compilation requirement. It MUST accept a configurable Gateway base
URL and MUST NOT contain a tracked credential or environment-specific host.

#### Scenario: Web files are served locally

- **WHEN** a static HTTP server serves `venueflow-web`
- **THEN** the application loads without installing packages or contacting a third-party runtime

### Requirement: Authentication session is bounded

The Web application SHALL support registration, login, one refresh attempt after an unauthorized
API response, and logout. Access and refresh tokens MUST remain in browser session storage, MUST
NOT be placed in URLs or logs, and SHALL be cleared when refresh or logout fails.

#### Scenario: Access token expires

- **WHEN** one authenticated API request returns 401 and a refresh token exists
- **THEN** the client performs one refresh and retries the original request once
- **AND** an unsuccessful refresh clears the session

### Requirement: Core user journey is operable

The Web application SHALL let an authenticated user resolve or create their profile, browse or
search campus resources, view slots, submit a booking application with a UUID idempotency key,
withdraw or cancel an active application, list booking history, and view notification records.
Approval and check-in actions MUST be presented only in the management workspace. Errors MUST
remain visible and MUST NOT be rendered as successful actions.

#### Scenario: User submits a reservation application

- **WHEN** a signed-in user chooses a resource slot and submits a valid quantity
- **THEN** the Web client creates one `PENDING_CONFIRMATION` booking
- **AND** history presents it as waiting for school approval

### Requirement: Web application provides dual campus workspaces

The Web application SHALL provide a user workspace for campus applicants and a management
workspace for authorized staff. Both workspaces MUST share the bounded authentication session,
Gateway configuration, error handling, and zero-build delivery model.

#### Scenario: A signed-in user changes workspace

- **WHEN** a signed-in management user follows the administration entry
- **THEN** the management workspace reuses the current session without placing tokens in a URL

### Requirement: Frontend verification is deterministic

Node built-in tests SHALL cover URL construction, authorization, envelope/error handling,
refresh-once behavior, and idempotency-key propagation without a browser, network, or installed
package.

#### Scenario: Repository verification runs

- **WHEN** the frontend test command executes
- **THEN** it uses only local mocked fetch responses and creates no external connection

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
