# Web Application Specification

## Purpose

Define the directly runnable browser application and its bounded user journey through Gateway.

## Requirements

### Requirement: Web application is dependency-free and directly runnable

The repository SHALL provide `venueflow-web` as browser-native HTML, CSS, and ES modules with no
runtime package dependency or compilation requirement. It MUST accept a configurable Gateway base
URL and MUST NOT contain a tracked credential or environment-specific host.

#### Scenario: Web files are served locally

- **WHEN** a static HTTP server serves `venueflow-web`
- **THEN** the application loads without installing packages or contacting a third-party runtime

### Requirement: Authentication session is bounded

The Web application SHALL support registration, login, one refresh attempt after an unauthorized
API response, and logout. Access and refresh tokens MUST remain in browser session storage, MUST
NOT be placed in URLs or logs, and SHALL be cleared when refresh or logout fails.

#### Scenario: Access token expires

- **WHEN** one authenticated API request returns 401 and a refresh token exists
- **THEN** the client performs one refresh and retries the original request once
- **AND** an unsuccessful refresh clears the session

### Requirement: Core user journey is operable

The Web application SHALL let an authenticated user resolve or create their profile, browse or
search resources, view slots, create a booking with a UUID idempotency key, confirm, cancel,
check in, list booking history, and view notification records. Errors MUST remain visible and
MUST NOT be rendered as successful actions.

#### Scenario: User completes a reservation workflow

- **WHEN** a signed-in user chooses a resource slot and submits a valid quantity
- **THEN** the Web client creates one pending booking and can invoke its valid lifecycle actions
- **AND** history refresh displays the resulting server state

### Requirement: Frontend verification is deterministic

Node built-in tests SHALL cover URL construction, authorization, envelope/error handling,
refresh-once behavior, and idempotency-key propagation without a browser, network, or installed
package.

#### Scenario: Repository verification runs

- **WHEN** the frontend test command executes
- **THEN** it uses only local mocked fetch responses and creates no external connection
