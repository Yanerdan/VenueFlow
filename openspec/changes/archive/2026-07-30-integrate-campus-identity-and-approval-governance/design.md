## Context

VenueFlow currently owns local credentials and RS256 access/refresh tokens in Auth Service, stores optional flat campus facts in User Service, and stores direct or two-stage approvers on each resource. Booking snapshots those fixed fields. The change crosses four databases and both web workspaces, while any real university IdP endpoints, secrets, claims, and directory contracts remain deployment-specific.

## Goals / Non-Goals

**Goals:**

- Make standards-based campus sign-in configurable without changing downstream JWT trust.
- Represent a school organization tree and auditable authoritative membership synchronization.
- Support one to five ordered approval stages with immutable booking snapshots.
- Preserve local demonstration login, legacy resources, and in-flight bookings.
- Show administrators whether external integrations are configured, healthy, stale, or failed.

**Non-Goals:**

- Claim that a real school is connected without its metadata and credentials.
- Store IdP passwords, proxy a school login form, or build a custom identity protocol.
- Support arbitrary workflow expressions, parallel approval, countersignature, delegation, or dynamic BPMN.
- Remove local accounts or rewrite already completed approval history.

## Decisions

### Auth remains the token issuer

Auth Service implements OIDC Authorization Code with PKCE and state/nonce validation. The browser receives a short-lived, single-use completion code and exchanges it for the existing VenueFlow token pair. External issuer and subject form the immutable binding key; downstream services continue trusting only VenueFlow JWTs.

This is preferred over teaching every service to trust external tokens, which would spread provider claim mapping and revocation policy across the system. Provider discovery, client credentials, claim mapping, and enabled state come from environment configuration; an incomplete provider is reported as unavailable.

### Organization synchronization uses an explicit canonical import contract

User Service owns `organization_unit`, `directory_membership`, and `directory_sync_run`. A system administrator or configured connector submits bounded canonical batches with source, external keys, parent keys, memberships, and a sync idempotency key. A full successful run may deactivate missing source-owned records; partial runs only upsert.

This provider-neutral boundary is preferred over hard-coding one school's proprietary API. A later connector can translate SCIM, LDAP exports, or school HTTP responses into the same canonical contract.

### Directory-owned facts are visibly authoritative

Profiles retain their stable external user identity and expose organization unit, campus ID, identity type, source, and last synchronization time. Self-service can still edit contact facts, but cannot overwrite directory-owned campus ID, identity type, or organization membership while the binding is active.

### Approval policies are normalized and snapshot per booking

Resource Service stores an approval policy and one to five ordered stages, each with an assigned VenueFlow external user ID and label. A resource points to a policy and responses expose the ordered stages. Direct and two-stage legacy fields are migrated into equivalent policies.

Booking Service stores an immutable ordered stage snapshot for every new booking. Authorization checks the current pending snapshot row; approval advances exactly one row, rejection ends the booking, and confirmation occurs only after the final row. Legacy bookings without rows continue through the existing fixed-field path until completed.

### Administration remains bounded

The current zero-dependency management workspace gains compact identity readiness, organization directory/sync, and ordered approval policy editors. Policies are limited to five sequential stages to avoid introducing a general workflow engine.

## Risks / Trade-offs

- [IdP claim differences] → Keep claim names configurable and expose readiness diagnostics without secrets.
- [Account-link takeover] → Bind only verified issuer/subject pairs and reject automatic linking when the mapped campus ID or username conflicts.
- [Directory sync removes valid data] → Only a successful explicitly full run may deactivate missing records; retain run counts and error summaries.
- [Cross-service migration mismatch] → Deploy additive migrations first and keep legacy read paths during the transition.
- [Approval edits affect active work] → Snapshot every booking chain and never mutate snapshots after submission.
- [More stages increase operator effort] → Bound policies to five sequential stages and show clear progress, assignee, and completed actions.

## Migration Plan

1. Apply additive Auth and User migrations; keep OIDC disabled by default.
2. Apply Resource approval-policy tables and backfill one policy per configured resource.
3. Apply Booking stage-snapshot tables and use them for new bookings while retaining legacy fallback.
4. Deploy management and applicant UI changes.
5. Configure a real OIDC provider and directory connector only in the target environment, run a dry synchronization, then enable sign-in.
6. Rollback disables external providers and new policy editing; additive tables remain readable and legacy local login remains available.

## Open Questions

- Each target school must supply its issuer metadata, allowed redirect URI, client registration, stable subject/campus claims, and organization export contract.
- Production policy must decide whether local password login remains available to emergency administrators only.
