## ADDED Requirements

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
