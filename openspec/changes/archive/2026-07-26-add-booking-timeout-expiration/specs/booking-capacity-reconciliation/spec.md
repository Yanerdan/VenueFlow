## MODIFIED Requirements

### Requirement: Allocation reconciliation releases only proven orphan capacity

For an open allocation intent, Booking SHALL query the existing Resource operation API using the
stored slot and allocation operation ID. A definitive absence MAY resolve the intent without a
repair. A matching allocation with a `PENDING_CONFIRMATION` or `CONFIRMED` Booking SHALL resolve
as consistent. A matching allocation without a successful Booking SHALL be released only with
the stored deterministic release operation ID and quantity.

An ambiguous release response MUST be resolved through Resource operation lookup and MUST NOT
cause a different release write. Booking SHALL resolve the intent as repaired only after the
release is proven. Conflicting or unknown facts MUST remain unresolved and create/update a
bounded issue. C14 orphan repair MUST NOT claim or expire a valid pending reservation; C15
expiration remains the sole deadline workflow.

#### Scenario: Process stops after Resource allocated capacity

- **WHEN** reconciliation proves the allocation exists but no successful Booking exists
- **THEN** it performs one effective deterministic release
- **AND** records the intent and repair outcome without creating a reservation

#### Scenario: Pending reservation owns the allocation

- **WHEN** reconciliation finds a matching pending reservation and allocation
- **THEN** it resolves the allocation intent as consistent
- **AND** leaves deadline processing to the expiration workflow

#### Scenario: Release response is lost

- **WHEN** Resource applies the repair release but Booking receives an ambiguous response
- **THEN** Booking queries the same release operation
- **AND** resolves the intent without issuing another distinct release

#### Scenario: Allocation facts conflict

- **WHEN** Resource returns an operation whose slot, type, or quantity does not match the intent
- **THEN** Booking performs no repair
- **AND** records a stable unresolved issue for operator review
