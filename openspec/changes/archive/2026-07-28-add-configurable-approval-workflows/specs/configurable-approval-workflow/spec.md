## ADDED Requirements

### Requirement: Resources choose a bounded approval policy

Resource Service SHALL support `DIRECT` and `TWO_STAGE` approval modes. Direct mode SHALL use one assigned approver; two-stage mode SHALL require distinct initial and final approver external user IDs.

#### Scenario: Manager configures two-stage approval
- **WHEN** an authorized manager selects two distinct eligible approvers
- **THEN** Resource persists the two-stage policy optimistically

#### Scenario: Manager repeats an approver
- **WHEN** both stages reference the same account
- **THEN** Resource rejects the invalid policy

### Requirement: Bookings snapshot their approval chain

Booking Service SHALL copy approval mode and assigned stage identities from the resource slot when creating a reservation so later resource edits do not change the in-flight chain.

#### Scenario: Resource policy changes after submission
- **WHEN** a manager edits the resource after a booking was created
- **THEN** the booking retains its original approval chain

### Requirement: Approval advances one bounded stage at a time

Booking Service SHALL keep a two-stage booking pending after initial approval, advance it to the final stage exactly once, and confirm it only after final approval.

#### Scenario: Initial approver approves
- **WHEN** the assigned initial approver confirms a two-stage pending booking
- **THEN** Booking records the action and advances the current stage without confirming the booking

#### Scenario: Final approver approves
- **WHEN** the assigned final approver confirms the second stage
- **THEN** Booking records the action and transitions the booking to `CONFIRMED`

#### Scenario: Either stage rejects
- **WHEN** the current assigned approver rejects with a bounded reason
- **THEN** Booking records the action and ends the booking using the existing cancellation behavior

### Requirement: Approval progress is visible

Booking APIs and web workspaces SHALL expose the current stage, total stages, and ordered bounded approval actions without credential secrets.

#### Scenario: Applicant views an in-progress chain
- **WHEN** the initial stage has approved but final approval is pending
- **THEN** the applicant sees stage two pending and the completed initial action
