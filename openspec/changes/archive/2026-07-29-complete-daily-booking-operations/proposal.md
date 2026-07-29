## Why

VenueFlow already completes the reservation lifecycle, but frequent users still need to scan flat lists, re-enter application data, and manage resources or opening hours one record at a time. The next increment should remove those daily frictions without expanding into deferred production-launch work.

## What Changes

- Let applicants narrow resources by date, capacity, category, and text, and present slots grouped by day.
- Add local draft recovery, booking-history filters, repeat-application prefilling, and actionable notification read state.
- Let resource managers edit core resource facts after creation.
- Add bounded recurring slot publication and CSV export for routine administration.
- Hide incomplete active resources from the applicant catalogue until their public facts are operationally usable.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `web-application`: Add date/capacity discovery, grouped availability, draft recovery, history actions, actionable inbox behavior, and management productivity controls.
- `resource-catalog`: Permit optimistic updates to a resource's core public facts.
- `resource-slot-management`: Support bounded recurring slot publication through the management workspace.
- `campus-administration`: Add resource editing, bulk opening-hour operations, and client-side operational export.
- `reliable-notification-consumption`: Extend the browser inbox with local read state and booking navigation without changing durable event consumption.

## Impact

- Resource Service receives one versioned core-facts update API.
- The dependency-free applicant and management workspaces gain focused interaction and rendering changes.
- Existing schemas, booking lifecycle, event contracts, and service boundaries remain unchanged.
- No new runtime dependency, database migration, SSO integration, organization sync, load testing, or release infrastructure is introduced.
