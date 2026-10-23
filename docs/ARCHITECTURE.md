# Architecture and Boundaries

## Runtime topology
- Cocos Creator client communicates with API using HTTPS and JSON.
- Admin SPA uses the same API with separate admin-authorized routes.
- Spring Boot contains transport controllers, application services, domain rules, persistence adapters and security.
- MySQL stores users, sessions/token metadata where applicable, game sessions, demo-credit ledger and audit records.
- External email provider is an adapter to be added; development mode must not leak reset tokens in production logs.

## Backend package proposal
`com.aagame.api`
- `config/`: security, CORS, serialization and properties
- `auth/`: registration, login, refresh/logout and password reset
- `user/`: profile and account status
- `wallet/`: demo-credit account, ledger and transactional operations
- `game/`: catalog, session lifecycle and game-specific rules
- `admin/`: protected administration use cases
- `audit/`: append-only audit event creation/query
- `common/`: API error model, request IDs and shared validation

Keep controllers thin. Put business logic in services; persistence writes should be transactional. Avoid circular dependencies and do not return JPA entities directly from controllers.

## Trust boundaries
1. Client input is untrusted, even when it comes from the official app.
2. Every protected request is authenticated and authorized server-side.
3. The database transaction decides whether a credit mutation succeeds.
4. Admin actions require role checks and audit records.
5. Configuration and secrets are injected at runtime, never committed.
6. Production endpoints require TLS, rate limiting, monitoring and backups.

## Game protocol
1. Client requests a game session with an idempotency key.
2. API validates user, game status and permitted virtual-credit stake/rules.
3. Server records the session and any stake ledger transaction atomically.
4. Server computes/validates result under documented demo rules.
5. API persists result and ledger changes transactionally.
6. Client renders the returned result; client animations are not authoritative.
7. Duplicate requests return the prior result rather than mutating credits twice.

For any crash-style demo, define whether the round is deterministic scripted simulation or server-generated randomness. For slots, define the result model and payout table in a versioned config. Do not let clients submit their own multiplier, win amount or balance.

## Reliability
- Add request correlation IDs and structured logs.
- Use database constraints for unique emails, unique idempotency keys and ledger references.
- Use health/readiness checks; readiness should eventually check required dependencies.
- Add timeout/retry policies only for safe idempotent operations.
- Use migrations; never rely on manually editing a live schema.

## Deployment shape
Local: Docker Compose for MySQL + API process.
Staging: containerized API, managed MySQL, TLS ingress, restricted secrets and test accounts.
Production should remain out of scope until privacy, legal, security and operational reviews are complete.
