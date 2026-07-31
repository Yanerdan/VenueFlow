## ADDED Requirements

### Requirement: Actionable showcase reservations preserve capacity facts

Every synthetic pending or confirmed reservation SHALL reference an open slot whose occupied quantity and allocation operation match the reservation quantity. Reseeding SHALL remove only reserved showcase and local-acceptance residue and SHALL leave unrelated user records untouched.

#### Scenario: Applicant cancels an active showcase reservation

- **WHEN** the reservation releases its reserved quantity
- **THEN** Resource recognizes the allocation operation, permits release on the open slot, and cannot underflow occupancy
