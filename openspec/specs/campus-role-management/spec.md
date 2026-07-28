# Campus Role Management Specification

## Purpose

Define bounded campus role administration and safe approver discovery owned by Auth Service.

## Requirements

### Requirement: System administrators browse bounded authentication accounts

Auth Service SHALL expose a bounded authentication-account directory containing stable user ID, username, campus role, token version, and update time only to a trusted `SYSTEM_ADMIN` context.

#### Scenario: System administrator loads accounts

- **WHEN** a trusted system administrator requests the account directory
- **THEN** Auth returns at most two hundred accounts without password hashes, refresh tokens, or lock details

#### Scenario: Non-administrator requests accounts

- **WHEN** an applicant, approver, or resource manager requests the account directory
- **THEN** Auth returns a stable forbidden response

### Requirement: System administrators assign a single campus role

Auth Service SHALL allow a trusted `SYSTEM_ADMIN` to assign one supported campus role to an account identified by its stable user UUID, increment the credential and token versions when the role changes, and return the updated account.

#### Scenario: Administrator promotes an approver

- **WHEN** a system administrator assigns `APPROVER` to an applicant account
- **THEN** Auth persists the role, advances token version, and requires the target account to log in again for the new role

#### Scenario: Administrator submits the existing role

- **WHEN** a system administrator assigns the role already held by the target account
- **THEN** Auth returns the unchanged account without an additional version advance

#### Scenario: Administrator attempts self-demotion

- **WHEN** a system administrator assigns a non-system-administrator role to their own account
- **THEN** Auth rejects the operation and preserves system administration access

### Requirement: Management workspace joins accounts and campus profiles

The management web application SHALL join authentication accounts with campus profiles by stable external user ID, display the current role, and allow a system administrator to save a supported role.

#### Scenario: Administrator edits a staff role

- **WHEN** a system administrator selects a new role for a directory user and saves it
- **THEN** the management workspace displays the persisted role and informs that the target user must log in again

### Requirement: Resource managers browse eligible approvers

Auth Service SHALL expose a bounded safe directory containing only accounts whose role is `APPROVER` or `SYSTEM_ADMIN` to trusted `RESOURCE_MANAGER` and `SYSTEM_ADMIN` contexts.

#### Scenario: Resource manager loads approver choices

- **WHEN** a trusted resource manager requests eligible approvers
- **THEN** Auth returns stable user IDs, usernames, and approval roles without exposing applicant accounts or credential secrets
