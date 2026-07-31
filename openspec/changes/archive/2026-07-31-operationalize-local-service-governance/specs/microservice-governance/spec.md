## ADDED Requirements

### Requirement: Governed local startup is repeatable and non-breaking

The repository SHALL provide an explicit governed local startup mode that initializes authenticated
Nacos, publishes tracked non-secret configuration, enables governance profiles, and leaves the
existing static URI startup unchanged when governance is not selected.

#### Scenario: Developer starts the default local stack

- **WHEN** governed mode is not selected
- **THEN** services use the existing static collaborator origins
- **AND** Nacos is not required for business liveness

#### Scenario: Developer starts governed mode

- **WHEN** governed mode is selected with valid local secrets
- **THEN** Nacos is healthy and every tracked Data ID is published and readable before Java
  services start
- **AND** no database password, JWT private key, or usable secret is published

### Requirement: Governed local acceptance proves instance failover

Governed local acceptance SHALL verify named service registration, at least two distinct Resource
instances, discovered routing, canonical trace identity, and continued bounded reads after one
Resource instance is stopped.

#### Scenario: One managed Resource instance is stopped

- **WHEN** two Resource instances are registered and the acceptance driver stops one exact PID
- **THEN** Nacos removes that instance within a bounded interval
- **AND** subsequent Gateway resource reads succeed through the remaining instance
- **AND** the driver restores the stopped instance without deleting data or volumes
