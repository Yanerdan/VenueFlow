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

### Requirement: Web application identifies the semester showcase

The local web application SHALL present the populated environment as a campus operations showcase and SHALL visibly disclose that its historical records are synthetic demonstration data.

#### Scenario: Reviewer opens the applicant or management entry

- **WHEN** the local showcase is loaded
- **THEN** the interface communicates a mature campus service identity and a concise demonstration-data disclosure

### Requirement: Applicant entry supports guided local evaluation

The local login surface SHALL identify a login-ready applicant demonstration account separately from the management account and SHALL allow the reviewer to fill the selected credential without exposing tokens or bypassing authentication.

#### Scenario: Reviewer chooses the applicant demonstration

- **WHEN** the reviewer activates the applicant demo entry
- **THEN** the login form is populated with the local applicant credential and normal Auth login remains required

### Requirement: Applicant workspace summarizes personal service state

The applicant workspace SHALL summarize active applications, approved upcoming use, recent messages, and campus-profile completeness from the authenticated user's current data.

#### Scenario: Returning applicant enters the workspace

- **WHEN** resources, reservations, notifications, and profile data load
- **THEN** the workspace presents concise personal status facts and a relevant next action

### Requirement: Resource discovery controls are functional

Applicant resource-category controls SHALL filter the loaded catalog, SHALL expose the current result count, and SHALL preserve text search as a bounded server-backed action.

#### Scenario: Applicant filters available spaces

- **WHEN** the applicant selects a category control
- **THEN** the resource list and result count reflect matching active spaces and the selected control is visibly active

### Requirement: Applicant history presents recognizable reservation facts

The applicant workspace SHALL resolve available slot and resource details for each listed reservation and SHALL present resource name, location, use period, approval progress, and submitted context instead of requiring the user to interpret an internal slot identifier.

#### Scenario: Applicant reviews reservation history

- **WHEN** booking, slot, and resource facts are available
- **THEN** each reservation card identifies where and when the requested use occurs and retains a readable fallback when a referenced record is unavailable

### Requirement: Application submission is guided and guarded

After a slot is selected, the application form SHALL display the selected resource and time, prefill available contact facts, summarize the expected approval path, prevent duplicate submission while the request is in progress, and navigate to the resulting personal history after success.

#### Scenario: Applicant submits a complete request

- **WHEN** the applicant confirms a selected slot and submits valid application details
- **THEN** one request is sent, success feedback identifies the next step, and the new reservation is visible in personal history

### Requirement: Applicant support context is visible

The applicant workspace SHALL explain profile completeness, notification purpose, responsible resource department, and the local support boundary without presenting invented real-time service claims.

#### Scenario: Applicant needs help completing a request

- **WHEN** the applicant reads the profile, resource, or help context
- **THEN** the workspace identifies what information is needed and which campus unit owns the resource

### Requirement: Applicants discover capacity by intended use

The applicant workspace SHALL filter active, publicly complete resources by category, text,
required capacity, and optional use date, and SHALL group matching open slots by calendar day.

#### Scenario: Applicant supplies intended date and capacity

- **WHEN** an applicant enters a date and attendee count
- **THEN** the workspace shows only resources with sufficient capacity and an open slot on that date

### Requirement: Applicant work is recoverable and reusable

The applicant workspace SHALL preserve incomplete application form values in browser-local storage,
SHALL restore them for the same identity, and SHALL let an applicant reuse facts from a previous booking.

#### Scenario: Interrupted application is restored

- **WHEN** an applicant returns after leaving an incomplete application in the same browser
- **THEN** the previous activity, contact, attendance, purpose, and note values are restored

#### Scenario: Historical application is reused

- **WHEN** an applicant chooses to reuse a historical booking
- **THEN** the workspace prefills reusable application facts and guides the applicant to choose a new open slot

### Requirement: Applicant history is filterable

The applicant workspace SHALL let users filter their booking history by status and text without
changing server-owned history.

#### Scenario: History filter is applied

- **WHEN** an applicant chooses a status or enters a resource, activity, or booking-number fragment
- **THEN** only matching loaded bookings are displayed and the empty state explains how to clear the filter

### Requirement: Applicants keep local favorite spaces
The applicant workspace SHALL let a signed-in identity add or remove loaded resources from a browser-local favorites set and SHALL provide a favorites-only discovery filter.

#### Scenario: Applicant favorites a resource
- **WHEN** an applicant activates the favorite control on a resource
- **THEN** the resource remains visibly favored for the same identity in the same browser

#### Scenario: Applicant filters favorites
- **WHEN** the applicant enables the favorites-only filter
- **THEN** only currently available loaded resources in that identity's favorites set are displayed

### Requirement: Confirmed reservations export to a calendar
The applicant workspace SHALL generate a calendar file for a confirmed reservation using its booking number, activity, resolved resource, location, and UTC slot interval.

#### Scenario: Applicant exports a confirmed reservation
- **WHEN** an applicant activates calendar export on a confirmed booking with a resolved slot
- **THEN** the browser downloads a valid `.ics` file containing the bounded reservation facts

### Requirement: Active reservations support guided rescheduling
The applicant workspace SHALL offer rescheduling only for cancellable reservations and SHALL explicitly disclose that the current reservation is withdrawn before replacement availability is chosen.

#### Scenario: Applicant confirms rescheduling
- **WHEN** an applicant confirms the reschedule action
- **THEN** the workspace cancels the original through Booking, preserves reusable application facts, and opens replacement slots for the same resource

#### Scenario: Cancellation fails
- **WHEN** Booking rejects the cancellation
- **THEN** the workspace leaves the original reservation visible and does not present the replacement workflow as successful

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
