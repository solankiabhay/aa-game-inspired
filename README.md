# AA Game Inspired — Product Prototype Blueprint

> **Purpose:** An original, educational demo-game platform prototype inspired by common mobile-game patterns. This is not a clone of proprietary source code, branding, artwork, or backend services.

## 1. Read this first

This repository is the single source of truth for what we are building and how to build it. Start here, then follow the documents in `docs/`.

- **Current stage:** working browser MVP + backend APIs for authentication, demo credits, demo game sessions, and a role-protected admin console. The native Cocos Creator app and production hardening remain incomplete.
- **Implemented so far:** `GET /api/health`, public game catalogue, Flyway migrations, BCrypt registration, opaque 24-hour Bearer sessions with hashed token storage, logout/profile endpoints, 1,000 non-redeemable welcome credits + ledger, server-generated demo-session scores, role-protected admin user status/audit APIs, API-connected browser client and admin console, H2 integration tests, GitHub Actions CI.
- **Not implemented yet:** rate limiting, email verification/password reset, production-grade unified errors, admin game-configuration UI/API, native Cocos Creator project, complete game mechanics, deployment and security/load testing.
- **Do not mistake a plan or placeholder for a finished feature.** Update this status whenever code is actually implemented and tested.

## 2. Product vision

Build a polished mobile-first demo gaming experience with account registration, a game lobby, three original mini-game prototypes, a virtual-credit wallet, profile/history pages, and a protected admin console. Users must always understand that credits are simulated and have no monetary value.

### MVP features

1. Email + password registration and login.
2. Password reset flow designed for a real email provider, with a development-only mock mode.
3. Game lobby with responsive cards and clear demo labels.
4. Crash-style game, slots-style game, and fishing-style game, all using virtual credits.
5. Server-authoritative demo-credit ledger; never trust a client-supplied balance or payout.
6. User profile, game history, session/logout behavior.
7. Admin dashboard for user status, game configuration, and audit events, protected by server-side roles.
8. API validation, structured errors, audit logs, automated tests, and CI checks.

### Explicit non-goals

- No real-money wagering, deposits, withdrawals, cash-outs, or payment QR integration.
- No copying third-party app code, private APIs, trademarks, or copyrighted art.
- No client-side secrets, embedded production credentials, or claims that obfuscation makes secrets safe.
- No claim of production readiness until security review, operational monitoring, load testing, and deployment checks are complete.

## 3. Architecture at a glance

```text
Cocos Creator client ── HTTPS/JSON ──> Spring Boot REST API ──> MySQL
                                           │
                                           ├── Auth & authorization
                                           ├── Demo-credit ledger
                                           ├── Game session/results
                                           ├── Admin/audit endpoints
                                           └── Health/metrics
Admin web UI ───────── HTTPS/JSON ─────────┘
```

The client renders screens and sends commands; the backend validates identity, permissions, game state, and virtual-credit transactions. MySQL is the source of truth for accounts, ledger entries, and persisted results.

## 4. Technology choices

- **Client:** Cocos Creator (TypeScript; select and pin a supported Creator version before implementation).
- **Backend:** Java 21 + Spring Boot 3.x, Maven, Bean Validation.
- **Database:** MySQL 8.x with versioned schema migrations (Flyway recommended).
- **Admin:** TypeScript web application; choose React/Vite or another framework before building screens.
- **Local infrastructure:** Docker Compose.
- **CI:** GitHub Actions for backend tests/build and client/admin checks when their manifests exist.
- **API contract:** REST JSON, documented in `docs/API.md`; consider OpenAPI generation once controllers stabilize.

## 5. Repository map

| Path | Responsibility |
|---|---|
| `backend/` | Spring Boot API, domain services, persistence, validation and tests |
| `client/` | Cocos Creator scenes, UI, API client and demo mini-games |
| `admin/` | Admin web dashboard (planned) |
| `database/` | Schema and migration design |
| `docs/` | Product spec, architecture, API, security, test plan and roadmap |
| `.github/workflows/` | CI automation |
| `docker-compose.yml` | Local MySQL for development |

## 6. Build sequence

### Phase 0 — Foundation
- [x] Repository and initial Spring Boot scaffold.
- [x] Health endpoint and local MySQL compose file.
- [x] Confirm Java 21/Spring Boot/Maven and add Flyway migrations.
- [x] Add local setup instructions and GitHub Actions backend CI.

### Phase 1 — Authentication and profiles
- [x] Registration with email validation and BCrypt password hashing.
- [x] Login with opaque random 24-hour Bearer session token; store only SHA-256 token hash.
- [x] Logout/revocation and profile/session validation.\n- [ ] Rate limits, password reset/verification and full role-based admin authorization.
- [x] Basic profile endpoint and auth integration tests.

### Phase 2 — Demo economy and game APIs
- [x] Virtual-credit account and initial welcome-credit ledger entry.
- [ ] General credit/debit operations and idempotency (credits are not currently wagered or spent).
- [x] Persistent demo sessions and server-generated completion scores.\n- [ ] Full game-specific state machines and result validation.
- [ ] Per-game rules, abuse controls and expanded deterministic game tests.

### Phase 3 — Cocos Creator client
- [ ] Navigation shell, login/register forms, loading/error states.
- [ ] Lobby and game cards.
- [ ] Three distinct playable demo prototypes with no cash value.
- [ ] Profile, credit history, settings, reconnect handling and accessibility checks.

### Phase 4 — Admin and operations
- [ ] Admin login using server-issued roles (not a hidden frontend route).
- [ ] User search/status, game config, audit log viewer.
- [ ] Admin actions logged with actor, reason, timestamp and before/after values.
- [ ] Health/metrics, backup/restore procedure and incident checklist.

### Phase 5 — Release hardening
- [ ] CI checks, dependency scanning, secret scanning and container scanning.
- [ ] API contract, integration, UI and load tests.
- [ ] Threat model, privacy review, production configuration review.
- [ ] Staging deployment and rollback rehearsal before any production release.

## 7. Local development

Prerequisites: Git, Docker Compose, Java 21, Maven (or use Maven Wrapper once added).

1. Copy `.env.example` to `.env` and replace local-only values.
2. Start MySQL: `docker compose up -d mysql`.
3. Run API from `backend/`: `mvn spring-boot:run`.
4. Verify: `curl http://localhost:8080/api/health`.
5. Run backend tests: `mvn test`.

The current health endpoint does not prove database connectivity or authentication is implemented. Do not expose development credentials or the local database port in production.

## 8. Quality gates

A feature is complete only when all apply:
- acceptance criteria are met;
- validation and authorization are enforced on the server;
- automated tests cover success, failure, and boundary cases;
- no secrets or private user data are committed;
- docs and API examples match actual behavior;
- CI passes and the change has been manually reviewed.

## 9. Documentation index

- [Product requirements and prototype screens](docs/PRODUCT_SPEC.md)
- [Architecture and module boundaries](docs/ARCHITECTURE.md)
- [API contract and endpoint plan](docs/API.md)
- [Data model and migration plan](docs/DATA_MODEL.md)
- [Security and threat model](docs/SECURITY.md)
- [Testing strategy and acceptance criteria](docs/TEST_PLAN.md)
- [Development roadmap](docs/ROADMAP.md)
- [Backend guide](backend/README.md)
- [Cocos client guide](client/README.md)
- [Admin guide](admin/README.md)

## 10. Contribution rule

Work in small, reviewable changes. Never mark a feature complete merely because a file exists. Record the exact test command and result in the pull request. Use original assets and neutral project branding.
