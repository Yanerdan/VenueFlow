## ADDED Requirements

### Requirement: Management workspace publishes bounded recurring slots
The management workspace SHALL let an authorized operator derive a bounded number of same-time opening slots on consecutive weeks and SHALL use the existing conflict-safe slot creation operation for every occurrence.

#### Scenario: Weekly slots are published
- **WHEN** an operator chooses a valid first interval and a recurrence count within the configured bound
- **THEN** the workspace creates each weekly occurrence and reports the number successfully published

#### Scenario: A generated occurrence conflicts
- **WHEN** one recurring occurrence is rejected by Resource Service
- **THEN** the workspace stops further creation, preserves prior successful occurrences, and identifies the failed occurrence
