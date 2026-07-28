## ADDED Requirements

### Requirement: Booking action APIs accept bounded review context

Booking Service SHALL accept a DTO body for management confirmation and rejection and for
cancellation. Management rejection MUST require `APPROVER` or `SYSTEM_ADMIN`; all review text
MUST be bounded, and action responses MUST return the existing safe success envelope with the
expanded reservation DTO.

#### Scenario: An applicant invokes management rejection

- **WHEN** an applicant calls the rejection action with a reason
- **THEN** Booking returns the established forbidden response and preserves reservation state

#### Scenario: A cancellation includes context

- **WHEN** an active reservation is cancelled with a bounded note
- **THEN** Booking preserves the note with the resulting cancelled reservation
