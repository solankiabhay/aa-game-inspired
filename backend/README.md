# Backend API

Java 21 + Spring Boot 3.5 + Maven REST API. MySQL 8.4 is used for local persistence; Flyway migrations V1–V4 are in `src/main/resources/db/migration` (users/sessions, demo credits/game sessions, admin audit events, game configuration).

## Run locally
From repository root:
1. Copy `.env.example` to `.env` and use local-only values.
2. Start the database: `docker compose up -d mysql`.
3. In `backend/`, run `mvn --batch-mode verify`.
4. Start the API with `mvn spring-boot:run`.
5. Open `http://localhost:8080/` for the browser client and `http://localhost:8080/admin/index.html` for the admin console.
6. Check `curl http://localhost:8080/api/health` and `curl http://localhost:8080/api/games`.

## Implemented endpoints
- `GET /api/health`: process health.
- `GET /api/games`: static original demo game catalogue.
- `POST /api/auth/register`: email/password validation, BCrypt hash, account + 1,000 demo credits + ledger entry in one transaction.
- `POST /api/auth/login`: 256-bit opaque Bearer token, 24-hour expiry; only SHA-256 token hash stored.
- `GET /api/auth/me`, `POST /api/auth/logout`: profile and session revocation.
- `GET /api/auth/me/demo-credits`, `GET /api/auth/me/ledger`: virtual balance and latest ledger entries.
- `POST /api/games/{gameId}/sessions`, `POST /api/games/sessions/{sessionId}/complete`, `GET /api/games/sessions/{sessionId}`: persistent demo session lifecycle. Score is generated on the server.
- `GET /api/admin/users`, `PATCH /api/admin/users/{userId}/status`, `GET /api/admin/audit-events`: role-protected admin APIs with audit records.
- `GET /api/admin/games`, `PATCH /api/admin/games/{gameId}/config`: role-protected game configuration with audit records.

## Authentication rate limits
A built-in fixed-window filter limits public authentication POST endpoints by the direct client IP:
- Login: 10 requests per 15 minutes.
- Registration: 5 requests per hour.
- Excess attempts receive HTTP 429, a `Retry-After` header, and a `RATE_LIMITED` response code.

Defaults can be overridden with Spring properties/environment variables:
- `APP_SECURITY_RATE_LIMIT_LOGIN_MAX_REQUESTS`
- `APP_SECURITY_RATE_LIMIT_LOGIN_WINDOW` (for example `15m`)
- `APP_SECURITY_RATE_LIMIT_REGISTER_MAX_REQUESTS`
- `APP_SECURITY_RATE_LIMIT_REGISTER_WINDOW` (for example `1h`)

This limiter is in-process. Before deploying multiple API instances, configure a shared gateway or distributed rate limiter (for example Redis), and only trust forwarded client-IP headers when the reverse proxy is explicitly configured to overwrite them.

## Tests and CI
`mvn --batch-mode verify` runs catalogue MVC, H2-backed auth/game-session/admin integration tests, static-page checks, rate-limit unit tests and a Flyway migration smoke test. GitHub Actions runs backend verification, CodeQL, and secret/dependency/misconfiguration scans for backend changes.

## Known limitations — not production ready
- Email verification, password reset, MFA, session listing and refresh-token rotation are not implemented.
- Persistent abuse monitoring, distributed rate limiting, production security headers/CSP, external secret management and production TLS/reverse-proxy configuration remain deployment work.
- Game catalog is in-memory; game scores are demo simulations, not complete game engines.
- A browser client and admin dashboard are served by Spring Boot; the native Cocos Creator app is not implemented yet.
- No production deployment, monitoring, backup/restore rehearsal, penetration test or load test has been completed.
- Demo credits are non-redeemable. There are no deposits, cash-outs, payments or real-money wagering flows.

Never commit secrets. Keep production secrets in a secret manager, and deploy only after the security checklist is complete.
