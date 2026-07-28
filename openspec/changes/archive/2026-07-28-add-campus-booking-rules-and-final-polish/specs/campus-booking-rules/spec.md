## ADDED Requirements

### Requirement: Resource owners configure bounded campus booking rules

The system SHALL allow a resource manager or system administrator to configure a booking notice, minimum advance hours, maximum advance days, and maximum duration minutes for a resource using optimistic concurrency.

#### Scenario: Rules are updated

- **WHEN** an authorized management user submits valid rules with the current resource version
- **THEN** the system persists the rules, increments the resource version, and returns the updated resource

#### Scenario: Rules are invalid

- **WHEN** a management user submits values outside the supported bounds
- **THEN** the system rejects the update without changing the resource

### Requirement: Booking time rules are enforced before allocation

The system SHALL validate a requested slot against its resource's booking-time rules before allocating capacity.

#### Scenario: Slot violates an advance or duration rule

- **WHEN** an eligible applicant submits a booking for a slot that is too soon, too far in the future, or too long
- **THEN** the system rejects the request as invalid and does not allocate slot capacity

#### Scenario: Slot satisfies all rules

- **WHEN** an eligible applicant submits a booking for a slot within all configured limits
- **THEN** booking processing continues through capacity allocation and approval selection

### Requirement: Booking rules are visible at the point of action

The system SHALL present each resource's notice and booking-time limits to applicants and authorized management users.

#### Scenario: Applicant reviews a resource

- **WHEN** an applicant views an available resource
- **THEN** the resource notice and a readable summary of booking limits are displayed

#### Scenario: Manager reviews a resource

- **WHEN** a resource manager or system administrator opens resource management
- **THEN** the current booking rules are displayed and can be edited
