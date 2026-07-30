## ADDED Requirements

### Requirement: Showcase booking quantities respect resource capacity

Synthetic semester reservations SHALL use a positive quantity no greater than the capacity of the joined resource.

#### Scenario: Local showcase data is reseeded

- **WHEN** reservation history is regenerated for a resource with positive capacity
- **THEN** every generated quantity is between one and that resource capacity inclusive
