## ADDED Requirements

### Requirement: Managers maintain public resource facts
The management workspace SHALL expose validated editing controls for a resource's core public facts and SHALL submit the resource version for optimistic concurrency.

#### Scenario: Manager corrects a resource
- **WHEN** a manager edits valid public facts and saves them
- **THEN** the workspace refreshes the catalogue with the returned or subsequently loaded facts

### Requirement: Managers export loaded operations
The management workspace SHALL export its authorized loaded booking records as UTF-8 CSV with stable column headers and spreadsheet-safe values.

#### Scenario: Loaded bookings are exported
- **WHEN** a manager requests an export
- **THEN** the browser downloads a CSV containing the loaded booking number, activity, resource, applicant, status, quantity, and review facts
