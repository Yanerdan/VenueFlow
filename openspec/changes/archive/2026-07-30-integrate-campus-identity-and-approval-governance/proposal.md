## Why

VenueFlow already demonstrates the complete booking journey, but real campus operation requires identities and departments to come from an authoritative school system and approval routes to reflect changing institutional rules. Adding integration-ready identity, directory, and workflow governance makes the product credible for departmental deployment without pretending that a specific school connection exists before credentials are supplied.

## What Changes

- Add configurable OIDC Authorization Code + PKCE campus sign-in alongside the existing local development login, with explicit provider status and safe account linking.
- Issue the existing VenueFlow access/refresh tokens after a verified external identity is mapped, so downstream services keep one trusted identity contract.
- Add a hierarchical organization directory and auditable, idempotent synchronization runs fed by a bounded provider-neutral import API.
- Mark directory-owned profile facts and prevent ordinary self-service edits from silently overriding authoritative campus identity data.
- Replace the fixed direct/two-stage resource setting with ordered approval policies of one to five stages while preserving legacy resources and in-flight bookings.
- Snapshot the selected policy and each assigned approver into every new booking, then advance, reject, scope, and display any configured stage.
- Add management workspace surfaces for identity-provider readiness, organization sync status, directory browsing, and ordered approval-policy editing.

## Capabilities

### New Capabilities

- `campus-federated-identity`: Configurable external OIDC login, verified identity mapping, local token issuance, provider readiness, and safe fallback boundaries.
- `organization-directory-sync`: Hierarchical organization units, authoritative membership facts, idempotent synchronization runs, and audit visibility.

### Modified Capabilities

- `auth-credential-token-lifecycle`: External verified identities can enter the existing VenueFlow token lifecycle without bypassing its revocation and rotation rules.
- `campus-user-directory`: Profiles expose authoritative organization membership and protect directory-owned identity fields.
- `configurable-approval-workflow`: Resource approval configuration and booking snapshots support one to five ordered stages rather than only fixed direct/two-stage modes.
- `campus-administration`: The management workspace administers provider readiness, directory synchronization, organizations, and ordered approval policies.
- `web-application`: Applicants can choose an enabled campus identity provider and all users can see authoritative organization and arbitrary approval-stage progress.

## Impact

- Auth Service gains OIDC provider configuration, external identity bindings, one-time login completion, and management readiness APIs.
- User Service gains organization-unit and directory-sync persistence, import/status APIs, and authoritative profile fields.
- Resource Service gains reusable ordered approval-policy persistence and resource policy assignment.
- Booking Service gains normalized per-booking stage snapshots and generic stage advancement while retaining legacy data compatibility.
- Gateway routes the new bounded endpoints; the browser applications add SSO entry, organization, sync, and workflow administration.
- New Flyway migrations and tests are required in Auth, User, Resource, and Booking. No real IdP URL, client secret, or school directory data is committed.
