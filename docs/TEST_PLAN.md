# Test Plan and Definition of Done

## Test layers
1. Unit tests: password policy, validation, game state transitions, credit calculations.
2. Repository/integration tests: migrations, uniqueness constraints, transactional rollback and idempotency.
3. API tests: status codes, DTO validation, generic auth errors, pagination, role boundaries.
4. Client tests: navigation, form validation, network errors, reconnect and screen scaling.
5. Admin tests: protected routes, filters, confirmations, audit visibility.
6. Security tests: unauthenticated access, privilege escalation, replay, injection-shaped inputs, Redis-backed rate limits, single-use email/reset tokens.
7. Performance tests: expected concurrent sessions, DB connection pool and p95 latency targets defined before load testing.
8. Release checks: dependency scan, secret scan, container scan, backup/restore and rollback.

## Minimum critical cases
- Duplicate normalized email is rejected.
- Incorrect password does not reveal account existence.
- Unverified account cannot sign in when email verification is enabled.
- Verification/reset tokens expire, can be consumed only once, and reset revokes existing sessions.
- Password-reset request returns a generic response for unknown accounts.
- Expired/revoked session cannot access protected resources.
- Player cannot call admin endpoints.
- Client cannot set a balance or arbitrary game result.
- Insufficient demo credits rejects a debit with no partial database update.
- Repeated idempotency key does not apply a second debit/credit.
- Concurrent requests cannot drive balance negative.
- Invalid game state transition is rejected.
- Admin mutation creates an audit record.
- Validation and malformed requests return a generic error envelope with request ID; unexpected exception handling still needs explicit coverage.

## Current validation
- [x] Maven `verify` and H2-backed integration tests run in GitHub Actions.
- [x] Tests cover catalogue, registration/login/logout, welcome-credit ledger, session lifecycle/history, admin authorization/status audit, static pages and Flyway migrations.
- [x] Tests cover email verification, password reset, token reuse rejection and session revocation after password change.
- [x] CI uses Redis in its MySQL-backed smoke test and verifies the login rate limit returns HTTP 429.
- [x] CI builds/runs the container against MySQL and runs a small k6 health smoke under concurrent load.
- [x] Live API smoke test against real MySQL and built container verified in GitHub Actions.
- [ ] Native Cocos Creator editor import, final scenes/prefabs, build and device test (a TypeScript scaffold exists; no editor build has been run).
- [ ] Production security/accessibility review.

Do not check a box until a real command or test run confirms it. Record command, environment and outcome in pull request notes.
