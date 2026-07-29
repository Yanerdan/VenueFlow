## Why

The populated platform looks operational from the management side, but a reviewer entering as an ordinary applicant still starts with an empty personal account and sees implementation identifiers instead of a coherent service journey. The local showcase needs a ready-to-use applicant identity and clearer decision support so that it feels credible without requiring setup knowledge.

## What Changes

- Provision a repeatable local applicant demo account whose personal history, approvals, and notifications are connected to the semester showcase.
- Add a guided demo-account entry on the login surface while keeping ordinary registration available.
- Turn the applicant landing view into a useful service dashboard with personal status metrics, actionable guidance, and working resource filters.
- Resolve reservation cards to human-readable resource names and use times instead of exposing only slot identifiers.
- Improve slot selection and application submission with selected-space context, prefilled contact details, a review summary, safe double-submit prevention, and an obvious post-submit destination.
- Improve message and profile views with useful counts, completeness guidance, and local service/help information.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `semester-showcase-data`: Connect a login-ready applicant identity to a representative subset of the synthetic semester history.
- `web-application`: Present a complete, legible applicant journey with dashboard context, filtering, resolved reservation facts, and guided submission.

## Impact

- Local showcase provisioning in `scripts/local-dev/seed.ps1` and `scripts/local-dev/semester-showcase.sql`.
- Applicant HTML, CSS, and JavaScript under `venueflow-web/`.
- Frontend API helper tests and local operator documentation.
- No public backend API or production authentication behavior changes.
