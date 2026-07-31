## ADDED Requirements

### Requirement: Oversized request rejection releases inbound buffers

When Gateway rejects a declared oversized request before routing, it SHALL consume and release received request buffers before completing the 413 response.

#### Scenario: Repeated oversized requests are rejected

- **WHEN** multiple declared oversized request bodies reach Gateway
- **THEN** every request returns 413 without reaching downstream and without leaking reference-counted buffers
