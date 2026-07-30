## 1. Campus Federated Identity

- [x] 1.1 Add Auth migrations and persistence for OIDC identity bindings and single-use login completions
- [x] 1.2 Add bounded provider readiness and external login initiation/completion APIs
- [x] 1.3 Map verified subjects to VenueFlow accounts and issue the existing access/refresh token pair
- [x] 1.4 Add Gateway routes, safe configuration, and Auth unit/integration coverage

## 2. Organization Directory

- [x] 2.1 Add User migrations and domain persistence for organization units, memberships, and sync runs
- [x] 2.2 Add idempotent partial/full canonical synchronization with hierarchy and deactivation validation
- [x] 2.3 Enrich profiles with authoritative membership and protect directory-owned self-service fields
- [x] 2.4 Add management directory, hierarchy, synchronization status APIs, and tests

## 3. Ordered Approval Policies

- [x] 3.1 Add Resource policy/stage migrations, legacy backfill, domain APIs, and optimistic assignment
- [x] 3.2 Expose ordered stages through resource and slot collaboration responses
- [x] 3.3 Add Booking stage-snapshot migration, generic authorization, advancement, rejection, and legacy fallback
- [x] 3.4 Expose ordered approval progress and extend Resource/Booking persistence tests

## 4. Web Governance Workspaces

- [x] 4.1 Add campus provider selection and explicit local-login fallback state
- [x] 4.2 Add integration readiness, organization hierarchy, and synchronization administration
- [x] 4.3 Replace direct/two-stage resource controls with an ordered one-to-five-stage policy editor
- [x] 4.4 Render authoritative organization facts and arbitrary approval progress in applicant and management views

## 5. Verification and Handoff

- [x] 5.1 Update environment examples, startup guidance, runbooks, and synthetic demonstration data
- [x] 5.2 Run service tests, browser journeys, repository quality checks, and strict OpenSpec validation
