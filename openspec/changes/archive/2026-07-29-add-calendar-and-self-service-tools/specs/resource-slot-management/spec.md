## ADDED Requirements

### Requirement: Browser bulk transitions remain bounded and optimistic
The management browser SHALL orchestrate bulk availability changes only over a selected resource's bounded loaded slot page and SHALL call the existing versioned status transition once per eligible slot.

#### Scenario: Loaded page is bulk transitioned
- **WHEN** an authorized operator confirms a target status
- **THEN** only loaded slots not already in that status are submitted with their individual expected versions

#### Scenario: One optimistic transition conflicts
- **WHEN** Resource Service rejects one submitted slot version
- **THEN** the browser stops the remaining sequence and reloads the selected resource's slot page
