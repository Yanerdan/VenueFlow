## ADDED Requirements

### Requirement: Web application provides dual campus workspaces

The Web application SHALL provide a user workspace for campus applicants and a management
workspace for authorized staff. Both workspaces MUST share the bounded authentication session,
Gateway configuration, error handling, and zero-build delivery model.

#### Scenario: A signed-in user changes workspace

- **WHEN** a signed-in management user follows the administration entry
- **THEN** the management workspace reuses the current session without placing tokens in a URL

## MODIFIED Requirements

### Requirement: Core user journey is operable

The Web application SHALL let an authenticated user resolve or create their profile, browse or
search campus resources, view slots, submit a booking application with a UUID idempotency key,
withdraw or cancel an active application, list booking history, and view notification records.
Approval and check-in actions MUST be presented only in the management workspace. Errors MUST
remain visible and MUST NOT be rendered as successful actions.

#### Scenario: User submits a reservation application

- **WHEN** a signed-in user chooses a resource slot and submits a valid quantity
- **THEN** the Web client creates one `PENDING_CONFIRMATION` booking
- **AND** history presents it as waiting for school approval
