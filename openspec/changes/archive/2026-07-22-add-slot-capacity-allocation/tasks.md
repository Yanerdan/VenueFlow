## 1. Persistence model and migration

- [x] 1.1 Inspect the existing V001/V002 schema, MyBatis entities, and MySQL test conventions before adding the allocation model.
- [x] 1.2 Add an immutable V003 Flyway migration that adds non-negative `allocated_quantity` to `resource_slot` and creates an allocation-operation ledger with operation id uniqueness, operation type, positive quantity, request fingerprint, timestamps, and supporting indexes.
- [x] 1.3 Add persistence entities, mapper methods, and locked slot lookups needed to read and update slot occupancy and allocation operations.
- [x] 1.4 Add focused persistence tests that verify V003 order, constraints, and no changes to V001 or V002.

## 2. Transactional capacity application service

- [x] 2.1 Define allocation, release, capacity-query, and operation-page command/result DTOs without exposing persistence entities.
- [x] 2.2 Implement allocation in one transaction: lock the slot, validate slot status and quantity, resolve the parent Resource capacity, enforce the capacity bound, persist the operation, and update occupied quantity.
- [x] 2.3 Implement idempotent allocation replay by returning the original successful result for the same request facts and returning a stable conflict for a reused operation id with different facts.
- [x] 2.4 Implement release in one transaction with matching idempotency behavior and a guard that prevents occupied quantity from becoming negative.
- [x] 2.5 Implement deterministic capacity and bounded allocation-operation queries with a default page size of 20 and a maximum of 100.
- [x] 2.6 Map missing or closed slot, invalid quantity, insufficient capacity, negative release, and idempotency conflicts to safe, stable domain errors.

## 3. Resource Service HTTP API

- [x] 3.1 Add DTO-only `POST /api/v1/resource-slots/{slotId}/allocations` and `POST /api/v1/resource-slots/{slotId}/releases` endpoints.
- [x] 3.2 Add DTO-only `GET /api/v1/resource-slots/{slotId}/capacity` and bounded operation-list endpoints with request validation.
- [x] 3.3 Integrate the new domain errors with the established error envelope without exposing SQL, credentials, or stack traces.
- [x] 3.4 Verify the increment introduces no Booking aggregate, authentication, Redis, message broker, Feign client, Nacos, or distributed transaction dependency.

## 4. Verification

- [x] 4.1 Add Docker-free unit and MVC tests for successful allocation/release, validation, error envelopes, capacity calculation, idempotent replay, and conflicting operation reuse.
- [x] 4.2 Extend the opt-in MySQL integration suite to verify V003, database constraints, allocation and release boundaries, and operation query ordering.
- [x] 4.3 Add a MySQL-backed concurrent-allocation test proving that concurrent requests cannot persist occupancy above the static Resource capacity.
- [x] 4.4 Run default `mvn clean verify` and ensure it requires no Docker; run `mysql-it` verification when Docker is available.

## 5. Acceptance review

- [x] 5.1 Manually exercise allocation, replay, conflicting operation reuse, release, capacity lookup, and operation lookup against a local Resource Service instance.
- [x] 5.2 Review the implementation and migration against the slot-capacity-allocation spec, including non-goals and safe-error requirements.
