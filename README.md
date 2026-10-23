# AA Game Inspired — Demo Game Platform

An original educational demo-game platform using a Spring Boot API, MySQL, browser client, admin console, and Cocos Creator TypeScript integration scaffold.

**Safety boundary:** demo credits have no monetary value. No real-money wagering, deposits, payments, withdrawals, cash-outs, or private payment QR integration is included.

## Current state

Implemented and CI-tested:
- Java 21 / Spring Boot API with MySQL and Flyway migrations.
- Email/password registration and login, BCrypt password hashing, opaque 24-hour sessions stored as hashes, logout/profile APIs, configurable email verification, and one-time password reset with session revocation.
- Non-redeemable demo credits and ledger, server-generated demo game sessions.
- Role-protected admin endpoints and audit events.
- Responsive browser arcade client with interactive Skyline Dash, Neon Sequence and Reef Ranger mini-challenges, account dashboard, demo-credit ledger, game history, and email-recovery screens; admin console.
- Client smoke tests validate inline JavaScript syntax, unique HTML IDs, DOM references and game/API wiring in CI.
- Authentication throttling (10 login attempts/IP/15m; 5 registration attempts/IP/hour), with an in-process local fallback and Redis-backed shared counters in the production template; baseline security headers and no-store headers for authentication responses.
- Backend integration tests, MySQL smoke test, Docker build/run, CodeQL, Gitleaks and Trivy CI workflows.
- Production deployment template using Caddy TLS, a baseline CSP, private MySQL/Redis network, database backup/restore scripts, and a k6 health smoke test. Operations CI validates Compose, Caddy syntax, and shell scripts.

## Quick start (local)

1. Install Docker Engine and Docker Compose v2.
2. Copy .env.example to .env; keep values local only.
3. Run docker compose up --build -d.
4. Visit http://localhost:8080/, http://localhost:8080/admin/index.html, and http://localhost:8080/api/health.
5. Use docs/ADMIN_BOOTSTRAP.md for the controlled admin setup process.

## Production template

See docs/DEPLOYMENT.md and deploy/README.md. The template is not a live deployment; it requires a domain, host, strong environment secrets, TLS validation, and operational monitoring.

## Repository map

- backend/ — Spring Boot API and tests
- client/web/ — browser MVP
- client/cocos/ — TypeScript integration scaffold; not yet a complete editor-generated Cocos Creator project
- admin/ — admin console
- database/ — backup/restore scripts and database documentation
- deploy/ — Caddy configuration and deployment notes
- loadtest/ — k6 health smoke test
- docs/ — product, API, security, test and operations docs
- .github/workflows/ — CI, CodeQL and security scans

## Important remaining release gates

This is not production-ready yet. MFA, refresh-token rotation, SMTP provider staging validation, external secrets manager integration, monitoring/alerting, production penetration/load tests, monitored backup-restore drills, and a real Cocos Creator editor build/device test remain. The production template enables verification/reset only after SMTP settings are supplied. Do not expose the prototype publicly until these are addressed.
