## ADDED Requirements

### Requirement: Existing notifications form a bounded read-only inbox

Notification Service SHALL expose a DTO-only newest-first inbox query over existing
`notification_record` facts for one positive internal user ID. `pageNumber` MUST be zero-based,
`pageSize` MUST default to 20 and be limited to 100, and the response SHALL include page metadata
without consumed-event hashes, failure facts, broker metadata, SQL, or credentials.

#### Scenario: A user opens their inbox

- **WHEN** a valid user ID requests a bounded page
- **THEN** Notification returns only that user's records ordered by creation time and ID descending

#### Scenario: Inbox is unavailable in the default profile

- **WHEN** Notification runs only its connection-free skeleton
- **THEN** no inbox controller or database connection is created
