## MODIFIED Requirements

### Requirement: Capacity state is queryable through bounded Resource Service DTOs

Resource Service SHALL provide `GET /api/v1/resource-slots/{slotId}/capacity`, a bounded
operation page for one slot, and
`GET /api/v1/resource-slots/{slotId}/allocation-operations/{operationId}` for one existing
allocation-ledger operation. Capacity responses MUST expose static capacity, occupied quantity,
available quantity, and slot status through DTOs only. Operation responses MUST expose only the
operation ID, operation type, quantity, bounded capacity result, and audit timestamps.

The direct lookup MUST require both slot ID and operation ID, MUST reject an operation belonging
to another slot as not found, and MUST use the established safe error envelope. Operation pages
MUST remain deterministic, default to 20, and reject or cap a size greater than 100. This
contract MUST NOT expose persistence Entities or permit Booking to access the Resource schema.

#### Scenario: A caller retrieves current capacity facts

- **GIVEN** a persisted slot with allocation operations
- **WHEN** a caller requests its capacity
- **THEN** Resource Service returns consistent static, occupied, and available quantities

#### Scenario: A caller resolves one operation outcome

- **GIVEN** an allocation operation exists for a slot
- **WHEN** its slot ID and operation ID are requested
- **THEN** Resource Service returns the bounded operation DTO

#### Scenario: An operation does not belong to the requested slot

- **GIVEN** an operation exists for another slot
- **WHEN** it is requested under the wrong slot ID
- **THEN** Resource Service returns the stable not-found error
- **AND** exposes no operation or persistence detail
