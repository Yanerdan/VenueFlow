## ADDED Requirements

### Requirement: Booking history is bounded and user-scoped

Booking Service SHALL expose a DTO-only newest-first reservation history query for one positive
internal user ID. `pageNumber` MUST be zero-based, `pageSize` MUST default to 20 and be limited to
100, and responses SHALL contain total elements, page metadata, and existing bounded reservation
DTOs without internal reconciliation, lease, SQL, or collaborator facts.

#### Scenario: A user requests booking history

- **WHEN** a valid user ID and bounded page are requested
- **THEN** Booking returns only that user's reservations ordered by creation time and ID descending
- **AND** no other user's reservation is present

#### Scenario: History page is oversized

- **WHEN** page size exceeds 100 or page number is negative
- **THEN** Booking returns a validation failure without querying an unbounded result
