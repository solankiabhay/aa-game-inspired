# Backend API

Java 21 + Spring Boot REST API using Maven.

## Prerequisites
- JDK 21
- Docker Compose for local MySQL (the current catalogue endpoint does not need the database)
- Maven 3.9+ or an installed Maven wrapper when added

## Run locally
From repository root:
1. Copy `.env.example` to `.env` and change local-only values.
2. Run `docker compose up -d mysql` if you need the local database for later development.
3. In this directory run `mvn spring-boot:run`.
4. Check `curl http://localhost:8080/api/health`.
5. Check the demo game catalogue with `curl http://localhost:8080/api/games`.

Run tests with `mvn --batch-mode verify`.

## Implemented
- `GET /api/health`: basic process health endpoint.
- `GET /api/games`: static catalogue of three original, demo-only games.
- MVC test for the catalogue response and expected game IDs.
- GitHub Actions CI on backend changes using Java 21 and Maven verify.

## Not yet implemented
Authentication, email verification/reset, persistent users, database migrations, authoritative game sessions/results, demo-credit ledger, authorization roles, admin routes, rate limiting, and production deployment. The current game catalogue is in-memory and must not be mistaken for a production game backend.

## Implementation conventions
- Keep controllers thin; use service classes for business rules.
- Validate request DTOs using Bean Validation.
- Use a central error response and never expose stack traces.
- Use transactions for ledger/result changes.
- Use migration scripts, not manual schema changes.
- Add tests with every behavior change.
- Keep secrets in environment variables or a secret manager.
- Demo credits must never be redeemable for money; do not add deposit/cash-out flows to this demo project.
