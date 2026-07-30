# organization-directory-sync Specification

## Purpose
TBD - created by archiving change integrate-campus-identity-and-approval-governance. Update Purpose after archive.
## Requirements
### Requirement: User Service owns a hierarchical organization directory
User Service SHALL persist source-owned organization units with stable external keys, bounded codes and names, optional parent relationships, active state, and synchronization timestamps, and MUST reject cycles or cross-source parent references.

#### Scenario: A valid organization hierarchy is imported
- **WHEN** a bounded batch contains parent-before-child or resolvable parent keys
- **THEN** User Service upserts the hierarchy and exposes ordered organization facts

### Requirement: Directory synchronization is idempotent and auditable
User Service SHALL accept a system-admin-only canonical synchronization batch with a unique source and run key, SHALL return the original result for a replay, and SHALL record start, completion, mode, counts, and bounded failure summary for every run.

#### Scenario: A synchronization run is replayed
- **WHEN** the same source and run key are submitted again
- **THEN** no duplicate organization or membership is created and the prior result is returned

#### Scenario: A partial run fails
- **WHEN** one record violates the canonical contract
- **THEN** the run is marked failed with bounded diagnostics and missing records are not deactivated

### Requirement: Full synchronization controls authoritative membership lifecycle
A successful explicitly full synchronization SHALL upsert provided memberships and deactivate missing records owned by that source; a partial synchronization MUST only upsert and MUST NOT deactivate absent records.

#### Scenario: Full synchronization removes a person from a unit
- **WHEN** a previously active source-owned membership is absent from a successful full run
- **THEN** User Service deactivates that membership while retaining its audit facts

### Requirement: Directory membership enriches campus profiles
User Service SHALL expose a user's active organization unit, authoritative source, and last synchronized time with the campus profile, while preserving the stable VenueFlow external user ID.

#### Scenario: A directory-bound user opens their profile
- **WHEN** an active membership exists
- **THEN** the profile identifies the authoritative organization and synchronization time
