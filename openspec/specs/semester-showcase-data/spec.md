# Semester Showcase Data

## Purpose

Define a repeatable synthetic semester dataset that makes the local VenueFlow environment useful for product demonstration and acceptance.
## Requirements
### Requirement: Local showcase represents a semester of campus operations

The local demonstration environment SHALL provide synthetic profiles, resources, slots, reservations, approval actions, and notifications spanning multiple campus departments and operational states.

#### Scenario: Fresh local environment is seeded

- **WHEN** an operator starts the local stack with seeding enabled
- **THEN** applicant and management views contain representative current and historical records without requiring manual entry

### Requirement: Showcase seeding is repeatable and scoped

The showcase seed SHALL use reserved stable identifiers and SHALL replace only showcase-owned rows when it is run again.

#### Scenario: Seed runs repeatedly

- **WHEN** the showcase seed is executed more than once
- **THEN** showcase totals remain stable and records outside the reserved showcase namespace are preserved

### Requirement: Showcase history supports operational reporting

The showcase dataset SHALL include non-uniform resource, department, status, attendance, review, and date distributions sufficient to exercise the existing operational report.

#### Scenario: Administrator opens the operations report

- **WHEN** the semester showcase has been seeded
- **THEN** the report displays meaningful totals, approval rate, attendance, rankings, department distribution, and recent review history

### Requirement: Showcase records are disclosed as synthetic

The local application and operator documentation SHALL identify showcase records as demonstration data rather than real personal or institutional records.

#### Scenario: Reviewer enters the local showcase

- **WHEN** the reviewer reads the platform identity or local runbook
- **THEN** the reviewer can determine that the populated semester history is synthetic

### Requirement: Showcase provides a login-ready applicant journey

The local showcase SHALL provision a reserved applicant credential through the public authentication flow and SHALL associate that identity with representative reservations, approval history, and notifications from the synthetic semester dataset.

#### Scenario: Reviewer enters as an applicant

- **WHEN** the local seed has run while Gateway is available and the reviewer uses the documented applicant credential
- **THEN** the applicant workspace displays a populated personal history without manual registration or data entry

#### Scenario: Applicant provisioning repeats

- **WHEN** the local seed is run more than once
- **THEN** the reserved applicant account and its associated showcase history remain stable without duplicate records

### Requirement: Showcase booking quantities respect resource capacity

Synthetic semester reservations SHALL use a positive quantity no greater than the capacity of the joined resource.

#### Scenario: Local showcase data is reseeded

- **WHEN** reservation history is regenerated for a resource with positive capacity
- **THEN** every generated quantity is between one and that resource capacity inclusive

### Requirement: Actionable showcase reservations preserve capacity facts

Every synthetic pending or confirmed reservation SHALL reference an open slot whose occupied quantity and allocation operation match the reservation quantity. Reseeding SHALL remove only reserved showcase and local-acceptance residue and SHALL leave unrelated user records untouched.

#### Scenario: Applicant cancels an active showcase reservation

- **WHEN** the reservation releases its reserved quantity
- **THEN** Resource recognizes the allocation operation, permits release on the open slot, and cannot underflow occupancy
