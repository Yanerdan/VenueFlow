## MODIFIED Requirements

### Requirement: Resources choose a bounded approval policy

Resource Service SHALL support an ordered approval policy containing one to five stages. Every stage SHALL have a bounded label, a distinct positive sequence number, and one eligible approver external user ID; a resource SHALL reference one active policy before accepting new bookings.

#### Scenario: Manager configures a multi-stage policy

- **WHEN** an authorized manager saves between one and five valid ordered stages
- **THEN** Resource persists the policy and stages optimistically

#### Scenario: Manager repeats a sequence

- **WHEN** two stages use the same sequence number or the policy exceeds five stages
- **THEN** Resource rejects the invalid policy

### Requirement: Bookings snapshot their approval chain

Booking Service SHALL copy the ordered stage labels, sequence numbers, and assigned external user IDs from the resource when creating a reservation so later resource or policy edits do not change the in-flight chain.

#### Scenario: Resource policy changes after submission

- **WHEN** a manager edits the resource policy after a booking was created
- **THEN** the booking retains its original ordered approval chain

### Requirement: Approval advances one bounded stage at a time

Booking Service SHALL keep a multi-stage booking pending after every non-final approval, advance it to the next stage exactly once, and confirm it only after the final configured stage approves.

#### Scenario: A non-final approver approves

- **WHEN** the assigned current approver confirms a pending booking
- **THEN** Booking records the action and advances one stage without confirming the booking

#### Scenario: The final approver approves

- **WHEN** the assigned final-stage approver confirms the pending booking
- **THEN** Booking records the action and transitions the booking to `CONFIRMED`

#### Scenario: Any stage rejects

- **WHEN** the current assigned approver rejects with a bounded reason
- **THEN** Booking records the action and ends the booking using the existing cancellation behavior

### Requirement: Approval progress is visible

Booking APIs and web workspaces SHALL expose the current stage, total stages, ordered stage labels and assignees, and ordered bounded approval actions without credential secrets.

#### Scenario: Applicant views an in-progress chain

- **WHEN** one or more non-final stages have approved
- **THEN** the applicant sees the current pending stage and every completed prior action
