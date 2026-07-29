## ADDED Requirements

### Requirement: Applicants discover capacity by intended use
The applicant workspace SHALL filter active, publicly complete resources by category, text, required capacity, and optional use date, and SHALL group matching open slots by calendar day.

#### Scenario: Applicant supplies intended date and capacity
- **WHEN** an applicant enters a date and attendee count
- **THEN** the workspace shows only resources with sufficient capacity and an open slot on that date

### Requirement: Applicant work is recoverable and reusable
The applicant workspace SHALL preserve incomplete application form values in browser-local storage, SHALL restore them for the same identity, and SHALL let an applicant reuse facts from a previous booking.

#### Scenario: Interrupted application is restored
- **WHEN** an applicant returns after leaving an incomplete application in the same browser
- **THEN** the previous activity, contact, attendance, purpose, and note values are restored

#### Scenario: Historical application is reused
- **WHEN** an applicant chooses to reuse a historical booking
- **THEN** the workspace prefills reusable application facts and guides the applicant to choose a new open slot

### Requirement: Applicant history is filterable
The applicant workspace SHALL let users filter their booking history by status and text without changing server-owned history.

#### Scenario: History filter is applied
- **WHEN** an applicant chooses a status or enters a resource, activity, or booking-number fragment
- **THEN** only matching loaded bookings are displayed and the empty state explains how to clear the filter
