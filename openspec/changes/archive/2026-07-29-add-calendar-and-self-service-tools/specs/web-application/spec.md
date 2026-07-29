## ADDED Requirements

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
