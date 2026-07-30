## ADDED Requirements

### Requirement: Applicant interactions preserve recognizable context

The applicant workspace SHALL use bounded future dates, complete status filters, status-appropriate action labels, and accessible slot names. A notification that references an application SHALL navigate to, expand, and focus that application's readable details.

#### Scenario: Applicant follows a reservation notification

- **WHEN** the applicant activates a notification linked to a reservation
- **THEN** the matching reservation is visible, expanded, focused, and described using recognizable resource and use-time facts

### Requirement: Catalog discovery favors visible local matches

Text discovery SHALL preserve bounded server search while preferring an exact case-insensitive name, location, description, or department match from the already loaded active catalog.

#### Scenario: Server search omits an obvious visible resource

- **WHEN** the entered text exactly matches facts in a loaded active resource
- **THEN** the matching local catalog resource remains discoverable

### Requirement: Management lists remain bounded and readable

Approval and personnel directories SHALL present at most 20 rows per client page with previous, next, current-page, and total-count context. Approval review SHALL identify the applicant, resource, location, use period, quantity, purpose, contact, and approval progress without requiring interpretation of internal identifiers.

#### Scenario: Administrator reviews a large semester dataset

- **WHEN** more than 20 matching approval or personnel records exist
- **THEN** the first page remains compact and the administrator can navigate all loaded bounded records

### Requirement: Advanced resource controls are progressive

The management resource catalog SHALL keep public-fact editing and governance configuration collapsed by default while preserving visible status, resource number, name, location, capacity, rules summary, and lifecycle action.

#### Scenario: Manager scans the resource catalog

- **WHEN** the resource view opens
- **THEN** each resource is concise and advanced forms are available only after explicit expansion
