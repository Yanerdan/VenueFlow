# Microservice Governance Specification

## Purpose

Define opt-in service discovery, configuration, load-balanced collaboration, trace propagation,
and deterministic verification boundaries for VenueFlow services.

## Requirements

### Requirement: Governance is opt-in and connection-free by default

Gateway, Auth, User, Resource, Booking, and Notification SHALL support an explicit `governance`
profile for Nacos discovery and non-secret configuration. Default skeleton startup MUST disable
discovery/config clients and MUST NOT require Nacos.

#### Scenario: Default verification runs without Nacos

- **WHEN** any executable service starts with its default profile
- **THEN** health probes start without a Nacos connection or registration attempt

### Requirement: Services register with bounded Nacos configuration

Governance mode SHALL register each service by `spring.application.name`, use the configured
namespace and `VENUEFLOW_GROUP`, import common and service-specific non-secret data IDs, and keep
credentials and JWT key material in environment variables.

#### Scenario: Governance configuration is inspected

- **WHEN** tracked governance configuration is reviewed
- **THEN** all Data IDs, namespace/group settings, and registration switches are explicit
- **AND** no usable secret or private key is present

### Requirement: Booking uses bounded service-discovered collaborators

In `persistence,governance`, Booking SHALL call User and Resource through explicit OpenFeign
clients backed by Spring Cloud LoadBalancer. Connect/read timeouts MUST be bounded, automatic
retry MUST be disabled, Resource writes MUST retain operation IDs, and ambiguous outcomes MUST use
the existing lookup/reconciliation path.

#### Scenario: Resource write times out

- **WHEN** a capacity write times out in governance mode
- **THEN** Booking does not blindly repeat the write
- **AND** resolves the outcome by operation identity or records it for reconciliation

### Requirement: Resource instances survive a single-instance loss

Governance mode SHALL allow at least two Resource instances with distinct ports/instance IDs and
client-side load balancing. Removing one healthy instance MUST leave the other able to serve
subsequent requests without changing business URLs.

#### Scenario: One Resource instance stops

- **WHEN** two Resource instances are available and one is removed
- **THEN** subsequent load-balanced reads reach the remaining instance

### Requirement: Trace identity propagates through synchronous calls

Services SHALL accept only a canonical UUID `X-Trace-Id`, otherwise generate one, bind it to the
request logging context, return it in the response, and forward it through Feign. Client-supplied
identity headers MUST NOT be forwarded by collaborator clients.

#### Scenario: Gateway request reaches a collaborator

- **WHEN** Gateway supplies a valid UUID trace and Booking calls Resource
- **THEN** Booking and Resource observe the same trace identifier
- **AND** responses expose that identifier without secrets

### Requirement: Governance verification is deterministic

Default verification SHALL cover profile boundaries, dependency rules, Feign timeout/retry
configuration, explicit discovery routes, trace propagation, two-instance selection, and
single-instance failure using only in-process fixtures.

#### Scenario: Root verification runs

- **WHEN** root `clean verify` executes
- **THEN** governance tests require no live Nacos, database, Redis, RabbitMQ, or remote endpoint
