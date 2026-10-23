# Backend API

Java 21 + Spring Boot REST API using Maven.

## Prerequisites
- JDK 21
- Docker Compose for local MySQL
- Maven (Maven Wrapper to be added)

## Run locally
From repository root:
1. Copy `.env.example` to `.env` and change local-only values.
2. Run `docker compose up -d mysql`.
3. In this directory run `mvn spring-boot:run`.
4. Check `curl http://localhost:8080/api/health`.

Run tests with `mvn test`.

## Current status
Only the application entry point and basic `GET /api/health` endpoint are implemented in the current scaffold. Authentication, database entities/migrations, game endpoints, authorization and admin routes are planned, not implemented.

## Implementation conventions
- Keep controllers thin; use service classes for business rules.
- Validate request DTOs using Bean Validation.
- Use a central error response and never expose stack traces.
- Use transactions for ledger/result changes.
- Use migration scripts, not manual schema changes.
- Add tests with every behavior change.
- Keep secrets in environment variables or a secret manager.
