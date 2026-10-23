# Implementation Roadmap

Status is based on committed code, not design documents. See the [deployment gate](DEPLOYMENT.md) before exposing the service publicly.

## P0 — Reproducible backend and container
- [x] Java 21 / Spring Boot / Maven backend and GitHub Actions CI.
- [ ] Add Maven Wrapper.
- [x] Flyway migrations and H2 migration smoke test.
- [x] Dockerfile and Compose stack for MySQL + API.
- [x] CI job exercises API against real MySQL and builds/runs the container (see latest workflow result).

## P1 — Authentication
- [x] Email validation, BCrypt password hashing and normalized unique emails.
- [x] Opaque 24-hour sessions, SHA-256 token storage, profile and logout.
- [x] Standard validation and malformed-JSON response envelopes.
- [x] Integration tests for register/login/profile/logout and bad credentials.
- [ ] Distributed rate limiting, email verification, password reset and refresh-token rotation.

## P2 — Demo credits and user activity
- [x] New users receive 1,000 non-redeemable demo credits and an initial ledger entry in one transaction.
- [x] Balance and latest ledger endpoints.
- [ ] General credit/debit operations, idempotency and concurrent-balance tests (no credits are currently spent or transferred).
- [x] Persistent demo game sessions, server-generated simulated scores and recent session history.
- [ ] Full per-game state machines and gameplay rules.

## P3 — Client
- [x] Browser client served by Spring Boot: register/login, lobby, wallet/ledger, play/complete rounds and recent history.
- [ ] Native Cocos Creator project, mobile scenes and device builds.

## P4 — Admin
- [x] Same-origin admin console and server-side ADMIN role checks.
- [x] User search, account suspension/reactivation, session revocation and audit viewer.
- [x] Game configuration API/editor with audited changes.
- [ ] Pagination, MFA/SSO and accessibility review.

## P5 — Release candidate
- [x] Backend CI, H2 integration tests, real-MySQL smoke test, Docker image build/run, Dependabot and CodeQL workflow configured.
- [x] Latest CI run passed Maven tests, Compose config validation, real-MySQL API smoke tests, Docker image build and container runtime smoke test.
- [ ] Add secret/container scanning, TLS/reverse proxy configuration, distributed rate limiting and production secrets management.
- [ ] Staging deploy, load tests, monitoring, backup/restore, rollback rehearsal, security/privacy/legal review.

## Release criteria
The project is a functional browser MVP, not a production release. Do not mark the native Cocos app, security hardening or deployment complete until their tests and acceptance criteria pass.
