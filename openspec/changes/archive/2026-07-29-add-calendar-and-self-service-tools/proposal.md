## Why

Applicants can now find and reuse reservations, but they still lack personal shortcuts and cannot carry confirmed use into their normal calendar. Resource operators can publish repeated slots, but maintaining a loaded schedule still requires changing every slot separately.

## What Changes

- Add identity-scoped favorite spaces and a favorites-only discovery filter.
- Let applicants download confirmed reservations as standards-compatible calendar files.
- Add a guided reschedule action that withdraws an eligible booking, preserves its application facts, and directs the user to choose a replacement slot.
- Present management slots in date groups with summary counts.
- Let managers open or close the currently loaded eligible slots as one explicitly confirmed, bounded browser operation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `web-application`: Add favorite discovery, calendar export, and guided rescheduling.
- `campus-administration`: Add schedule summaries and bounded bulk slot status operations.
- `resource-slot-management`: Define browser-orchestrated bulk availability transitions over existing optimistic APIs.

## Impact

- The dependency-free applicant and management browser workspaces are updated.
- Existing Resource and Booking APIs remain unchanged and authoritative.
- Personal favorites remain local to the signed-in identity and browser.
- No database migration, runtime dependency, or deferred production-launch scope is introduced.
