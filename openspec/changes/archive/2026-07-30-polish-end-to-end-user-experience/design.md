## Context

The application already supports the complete booking and administration journey. Browser acceptance exposed presentation and navigation friction rather than missing domain services.

## Decisions

- Preserve the dependency-free web client and improve existing views in place.
- Prefer exact local catalog matches when server search is narrower than the visible catalog.
- Load bounded management pages, then paginate the combined result in the browser for predictable 20-row tables.
- Keep detailed resource governance controls collapsed until explicitly requested.
- Generate showcase quantities from the joined resource capacity so every seeded booking remains credible.

## Risks

- Loading all bounded management pages adds several requests for unusually large local datasets. The current demonstration dataset is small and each server request remains capped at 100 records.
- Browser-native confirmation dialogs are retained for destructive actions; automated acceptance avoids confirming destructive operations.

## Verification

- Applicant and administrator browser walkthroughs, including validation, search, filtering, booking, approval, notifications, pagination, and empty/error states.
- JavaScript syntax checks, frontend tests, local full-chain smoke test, strict OpenSpec validation, and console error inspection.
