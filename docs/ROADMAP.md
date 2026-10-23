# Implementation Roadmap

## P0 — Make the repository reproducible
- Confirm Java/Maven versions and pin toolchain.
- Add Maven Wrapper.
- Add CI for backend build/test.
- Confirm MySQL startup and API smoke test.
- Add Flyway baseline and test profile.

## P1 — Authentication
- Define API DTOs and error model.
- Implement registration, password hashing, login and safe session management.
- Add profile endpoint, logout/revocation, password reset adapter.
- Add integration tests and rate limiting.

## P2 — Demo-credit ledger
- Add account initialization and ledger migration.
- Implement transactional, idempotent credit operations.
- Add ledger pagination and concurrency tests.

## P3 — Game service and Cocos UI
- Publish game catalog.
- Implement server-authoritative state machine and versioned rules for each demo.
- Build original lobby and mini-game scenes.
- Connect API client and error/loading states.

## P4 — Admin
- Choose admin SPA framework and pin version.
- Implement admin authorization, user lookup/status, game config and audit viewer.
- Add admin UI tests and accessibility pass.

## P5 — Release candidate
- CI quality/security gates, dependency/secret scanning.
- Staging deploy, load tests, monitoring, backup/restore, rollback.
- Security/privacy/legal review.

## Release criteria
No phase is considered done until its acceptance criteria and tests pass. Keep this roadmap aligned with actual repository code.
