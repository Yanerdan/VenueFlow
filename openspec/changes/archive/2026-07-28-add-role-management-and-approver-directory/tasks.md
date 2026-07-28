## 1. Auth role administration

- [x] 1.1 Add bounded account listing and UUID-based role update persistence
- [x] 1.2 Add system-admin-only role management service and HTTP endpoints with self-demotion protection
- [x] 1.3 Add Auth unit and controller tests for listing, promotion, no-op, forbidden access, and self-demotion

## 2. Trusted routing and directory access

- [x] 2.1 Require authentication for Auth management routes while retaining public lifecycle endpoints
- [x] 2.2 Allow resource managers to read the bounded User personnel directory
- [x] 2.3 Add focused Gateway and User authorization tests

## 3. Management user experience

- [x] 3.1 Join Auth accounts and User profiles in the management frontend
- [x] 3.2 Add system-admin role editing to the personnel directory
- [x] 3.3 Replace resource approver UUID inputs with eligible personnel selects

## 4. Acceptance

- [x] 4.1 Extend local smoke and documentation for role assignment and directory-selected ownership
- [x] 4.2 Run targeted tests, full live persistence acceptance, browser verification, and strict OpenSpec validation
