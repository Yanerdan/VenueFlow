## ADDED Requirements

### Requirement: Windows verification documents executable lock boundaries

Local documentation SHALL state that `clean verify` and executable repackaging require VenueFlow service processes to be stopped on Windows.

#### Scenario: Developer verifies after running the local stack

- **WHEN** executable service JARs are still running
- **THEN** the instructions direct the developer to stop the stack before invoking the packaging gate
