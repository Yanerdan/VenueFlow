## ADDED Requirements

### Requirement: Management schedule is date-oriented
The management workspace SHALL group a selected resource's loaded slots by local calendar date and SHALL summarize total, open, and closed counts.

#### Scenario: Operator selects a resource
- **WHEN** Resource Service returns the bounded slot page
- **THEN** the workspace presents date groups and status counts derived from those rows

### Requirement: Operators maintain loaded schedule availability in bulk
The management workspace SHALL let an authorized operator explicitly confirm opening or closing all eligible slots in the selected loaded page and SHALL use each slot's optimistic status API.

#### Scenario: Operator closes loaded open slots
- **WHEN** the operator confirms bulk closure
- **THEN** the workspace closes eligible rows sequentially and refreshes the schedule

#### Scenario: Bulk transition partially fails
- **WHEN** a slot transition fails after earlier transitions succeeded
- **THEN** further transitions stop and the workspace reports the successful count and refreshes authoritative state
