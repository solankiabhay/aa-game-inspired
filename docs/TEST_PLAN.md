# Test Plan and Definition of Done

## Test layers
1. Unit tests: password policy, validation, game state transitions, credit calculations.
2. Repository/integration tests: migrations, uniqueness constraints, transactional rollback and idempotency.
3. API tests: status codes, DTO validation, generic auth errors, pagination, role boundaries.
4. Client tests: navigation, form validation, network errors, reconnect and screen scaling.
5. Admin tests: protected routes, filters, confirmations, audit visibility.
6. Security tests: unauthenticated access, privilege escalation, replay, injection-shaped inputs, rate limits.
7. Performance tests: expected concurrent sessions, DB connection pool and p95 latency targets defined before load testing.
8. Release checks: dependency scan, secret scan, container scan, backup/restore and rollback.

## Minimum critical cases
- Duplicate normalized email is rejected.
- Incorrect password does not reveal account existence.
- Expired/revoked session cannot access protected resources.
- Player cannot call admin endpoints.
- Client cannot set a balance or arbitrary game result.
- Insufficient demo credits rejects a debit with no partial database update.
- Repeated idempotency key does not apply a second debit/credit.
- Concurrent requests cannot drive balance negative.
- Invalid game state transition is rejected.
- Admin mutation creates an audit record.
- Unexpected exceptions return a generic response and request ID without stack trace.

## Current validation
- [x] Maven `verify` and H2-backed integration tests run in GitHub Actions.
- [x] Tests cover catalogue, registration/login/logout, welcome-credit ledger, session lifecycle/history, admin authorization/status audit, static pages and Flyway migrations.
- [ ] Docker Compose startup against a real MySQL container verified end-to-end.
- [ ] Live API smoke test against MySQL verified.
- [ ] Native Cocos Creator editor build and device test (client not implemented).
- [ ] Production security/accessibility review.

Do not check a box until a real command or test run confirms it. Record command, environment and outcome in pull request notes.
