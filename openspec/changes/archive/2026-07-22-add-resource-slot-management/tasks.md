## 1. Persistence migration and domain boundary

- [x] 1.1 Inspect the existing C04 catalog entities, mapper conventions, profile configuration, and MySQL integration suite; preserve the default Docker-free `skeleton` profile and the narrow Resource Service dependency set.
- [x] 1.2 Add immutable `V002__add_resource_slots.sql` under Resource Service Flyway migrations to create `resource_slot` with its Resource foreign key, UTC temporal columns, `OPEN`/`CLOSED` status constraint, optimistic-lock version, timestamps, unique resource/time-range key, and resource/time-range lookup indexes; do not modify V001.
- [x] 1.3 Add the ResourceSlot persistence entity, status enum, mapper, and MyBatis queries needed for identifier lookup, deterministic intersecting-window paging, overlap detection, and optimistic status update.
- [x] 1.4 Implement a transactional Resource-row locking path for slot creation so concurrent writes for the same Resource cannot create overlapping half-open time ranges.

## 2. Slot application behavior

- [x] 2.1 Add request, response, and page DTOs for slot creation, status update, detail retrieval, and bounded time-window listing; accept offset date-times and normalize stored values to UTC.
- [x] 2.2 Implement slot creation for an existing ACTIVE Resource, including strict `endAt > startAt` validation, same-resource overlap rejection, and initial `OPEN` status/version/audit fields.
- [x] 2.3 Implement slot detail retrieval and resource-owned time-window listing with required valid window inputs, half-open interval intersection semantics, deterministic `startAt`/id order, default page size 20, and maximum page size 100.
- [x] 2.4 Implement explicit `OPEN <-> CLOSED` slot status transitions guarded by `expectedVersion`; reject missing slots, invalid transitions, and stale writes without changing stored state.
- [x] 2.5 Extend the existing exception/error mapping with stable safe codes for slot not found, inactive Resource, slot overlap, temporal validation, invalid slot state transition, and stale slot version while preserving the required error envelope.

## 3. HTTP contract

- [x] 3.1 Add `POST /api/v1/resources/{resourceId}/slots` and `GET /api/v1/resources/{resourceId}/slots` controller endpoints that delegate only to application services and never expose persistence entities.
- [x] 3.2 Add `GET /api/v1/resource-slots/{slotId}` and `PATCH /api/v1/resource-slots/{slotId}/status` controller endpoints with DTO validation and the established safe error behavior.
- [x] 3.3 Confirm no endpoint or implementation adds Booking, allocation/release, availability counters, recurring schedules, template generation, authentication, messaging, or new infrastructure clients.

## 4. Automated verification

- [x] 4.1 Add Docker-free unit tests for UTC normalization, strict temporal validation, Resource status eligibility, overlap logic including boundary-adjacent slots, and optimistic lifecycle transitions.
- [x] 4.2 Add Docker-free MVC/controller tests for successful slot APIs, deterministic bounded listing, validation failures, missing/inactive Resource, overlap conflicts, invalid transitions, stale versions, and safe error envelopes.
- [x] 4.3 Extend `ResourceCatalogMysqlSuite` (and its `mysql-it` POM include if needed) to verify V002 migration, real MySQL foreign-key and status constraints, slot create/list behavior, same-resource overlap rejection, and optimistic status transitions.
- [x] 4.4 Run `./mvnw clean verify` with JDK 21 and fix any failures without requiring Docker.
- [x] 4.5 With Docker available, run `./mvnw -Pmysql-it clean verify` and confirm the opt-in isolated MySQL suite passes.

## 5. Manual acceptance and scope review

- [x] 5.1 Start the Resource Service using the explicit persistence profile and local non-tracked database environment values; create an ACTIVE Category/Resource and then create, retrieve, list, close, and reopen a slot through HTTP.
- [x] 5.2 Verify an invalid interval, overlapping slot, unknown/inactive Resource, oversized or missing list window, and stale lifecycle update return stable safe error envelopes with no SQL, credentials, or stack traces.
- [x] 5.3 Inspect the migration, dependency tree, configuration, APIs, and automated-test split to confirm V002 is additive, V001 is unchanged, default startup stays infrastructure-independent, and no booking-domain or prohibited infrastructure behavior was introduced.
