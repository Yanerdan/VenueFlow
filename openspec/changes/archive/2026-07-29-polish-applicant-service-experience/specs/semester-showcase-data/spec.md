## ADDED Requirements

### Requirement: Showcase provides a login-ready applicant journey

The local showcase SHALL provision a reserved applicant credential through the public authentication flow and SHALL associate that identity with representative reservations, approval history, and notifications from the synthetic semester dataset.

#### Scenario: Reviewer enters as an applicant

- **WHEN** the local seed has run while Gateway is available and the reviewer uses the documented applicant credential
- **THEN** the applicant workspace displays a populated personal history without manual registration or data entry

#### Scenario: Applicant provisioning repeats

- **WHEN** the local seed is run more than once
- **THEN** the reserved applicant account and its associated showcase history remain stable without duplicate records
