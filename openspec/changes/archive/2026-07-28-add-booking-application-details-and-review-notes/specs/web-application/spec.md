## ADDED Requirements

### Requirement: Applicant workspace captures complete application context

The applicant workspace SHALL collect activity title, purpose, contact name, contact phone, and
optional note before creating a reservation and SHALL display those facts in booking history.

#### Scenario: An applicant submits a detailed request

- **WHEN** the applicant selects a slot and completes the bounded application form
- **THEN** the resulting booking card displays the submitted application context

### Requirement: Management workspace reviews application detail

The management workspace SHALL provide a readable application detail panel and SHALL let
authorized approvers confirm with an optional note or reject with a required reason. The visible
result MUST distinguish applicant cancellation from management rejection when review context is
available.

#### Scenario: An approver rejects an unsuitable activity

- **WHEN** an approver opens an application, enters a reason, and rejects it
- **THEN** the workspace displays the cancelled state together with the rejection reason
