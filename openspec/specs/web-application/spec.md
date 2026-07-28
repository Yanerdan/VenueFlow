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

### Requirement: Applicant workspace captures complete application context

The applicant workspace SHALL collect activity title, purpose, contact name, contact phone, and
optional note before creating a reservation and SHALL display those facts in booking history.

#### Scenario: An applicant submits a detailed request

- **WHEN** the applicant selects a slot and completes the bounded application form
- **THEN** the resulting booking card displays the submitted application context

### Requirement: Management workspace reviews application detail

The management workspace SHALL provide a readable application detail panel and SHALL let
authorized approvers confirm with an optional note or reject with a required reason. The visible
result MUST distinguish applicant cancellation from management rejection when review context is
available.

#### Scenario: An approver rejects an unsuitable activity

- **WHEN** an approver opens an application, enters a reason, and rejects it
- **THEN** the workspace displays the cancelled state together with the rejection reason

### Requirement: Management workspace configures and presents responsibility

The zero-build management workspace SHALL provide bounded resource ownership controls and SHALL
present assignment facts in resource records and booking review details. It MUST show unassigned
facts explicitly rather than inventing responsibility.

#### Scenario: A system administrator assigns a resource

- **WHEN** the administrator selects an approver and saves the resource
- **THEN** the refreshed resource card and later booking review show the persisted assignment

### Requirement: Web application presents booking rules and role-appropriate management

The web application SHALL display resource booking rules to applicants, provide rule editing to authorized resource management roles, and hide management sections that are not applicable to the signed-in role.

#### Scenario: Applicant prepares a reservation

- **WHEN** an applicant views a resource or starts a reservation
- **THEN** the current booking notice and time limits are visible

#### Scenario: Management user enters the workspace

- **WHEN** a resource manager, approver, or system administrator enters management
- **THEN** navigation and management actions are limited to sections applicable to that role
