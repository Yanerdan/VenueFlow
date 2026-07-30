## Why

End-to-end browser acceptance found several small but disruptive issues: catalog search could miss an obvious local match, messages did not reveal the referenced application, long management lists obscured daily work, and synthetic reservations could exceed resource capacity. These details make an otherwise complete platform feel unreliable.

## What Changes

- Make applicant discovery, date selection, status filtering, message navigation, and reservation actions clearer and safer.
- Add bounded pagination and compact default presentation to management lists.
- Present resource, time, applicant, workflow, and directory-sync facts in human-readable form.
- Keep synthetic semester reservations within each resource's capacity.
- Improve form semantics and accessible names without adding a framework or new runtime dependency.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `web-application`: Refine applicant and management interaction details for comfortable end-to-end use.
- `semester-showcase-data`: Keep generated reservation quantities consistent with resource capacity.

## Impact

- Static applicant and management web assets under `venueflow-web/`.
- Local showcase SQL under `scripts/local-dev/`.
- No public backend API, database migration, or production integration changes.
