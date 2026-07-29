## ADDED Requirements

### Requirement: Browser inbox tracks local attention state
The applicant workspace SHALL track read notification identifiers per signed-in identity in browser-local storage and SHALL allow a notification carrying a booking reference to navigate to the corresponding booking history.

#### Scenario: Notification is marked read
- **WHEN** an applicant opens a notification
- **THEN** that notification is visually marked read for the same identity in the same browser

#### Scenario: Notification references a booking
- **WHEN** an applicant opens a notification with a booking reference
- **THEN** the workspace presents booking history focused on that booking
