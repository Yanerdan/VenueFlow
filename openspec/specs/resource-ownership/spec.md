# Resource Ownership Specification

## Purpose

Define Resource-owned organizational responsibility for campus spaces.

## Requirements

### Requirement: Resource owns bounded organizational responsibility

Resource Service SHALL store an optional owning department and assigned approver external user ID
for each resource. Authorized management updates MUST be length-bounded, optimistic, and returned
through resource DTOs without reading another service database.

#### Scenario: A manager assigns resource responsibility

- **WHEN** a valid ownership update uses the current resource version
- **THEN** Resource persists the department and approver identifier and advances the version

#### Scenario: An existing resource has no assignment

- **WHEN** a resource created before the additive migration is read
- **THEN** it remains readable with absent ownership fields

### Requirement: Slot detail exposes parent resource responsibility

Resource Service SHALL include the slot's resource identifier and current bounded ownership facts
in its single-slot collaboration response.

#### Scenario: Booking resolves an assigned slot

- **WHEN** Booking reads a resource slot before reservation creation
- **THEN** the response identifies its resource, owning department, and assigned approver
