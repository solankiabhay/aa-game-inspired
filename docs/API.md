# REST API Contract

Base path: `/api`. JSON request/response bodies. Use HTTPS outside local development.

## Implemented

### GET /api/health
Returns `{"status":"UP","service":"aa-game-api"}`. Process health only; not a database readiness check.

### GET /api/games
Returns the static public catalog of three original demo-only games. Catalogue is currently in memory, not database-configurable.

### Authentication and user data
- POST `/api/auth/register` — validates email and a 12–72 character password, stores a BCrypt password hash, creates a user, and atomically grants 1,000 non-redeemable demo credits with a ledger entry.
- POST `/api/auth/login` — returns an opaque 256-bit random Bearer token with a 24-hour expiry. Only its SHA-256 hash is stored in the database.
- GET `/api/auth/me` — authenticated profile.
- POST `/api/auth/logout` — revokes the presented session token.
- GET `/api/auth/me/demo-credits` — authenticated demo-credit balance and `redeemable:false`.
- GET `/api/auth/me/ledger` — latest 100 demo-credit ledger entries.

Example registration:
```json
{"email":"player@example.com","password":"StrongPassword123!"}
```
Example login response:
```json
{"accessToken":"opaque-token","tokenType":"Bearer","expiresAt":"ISO-8601 timestamp"}
```
Send the returned token as `Authorization: Bearer <token>`. Never log passwords or session tokens.

### Game sessions
- POST `/api/games/{gameId}/sessions` — authenticated session start for `skyline-crash`, `neon-reels`, or `reef-ranger`.
- POST `/api/games/sessions/{sessionId}/complete` — completes an active session; score is generated server-side and cannot be chosen by the client. A completed session cannot be completed again.
- GET `/api/games/sessions/{sessionId}` — retrieves a session belonging to the authenticated user.

These are demo simulation endpoints, not production game mechanics. Demo credits are not wagered, transferred, sold or redeemable.

## Planned authentication hardening
- Rate limiting and abuse controls for registration/login.
- Email verification, password reset, session listing/revocation, refresh tokens, and stronger account recovery.
- Standardized validation error response for every exception type.

## Planned admin
- GET `/api/admin/users?query=...`
- PATCH `/api/admin/users/{userId}/status`
- GET/PATCH `/api/admin/games` and `/api/admin/games/{gameId}/config`
- GET `/api/admin/audit-events`

Admin mutations must enforce an admin role, validate changes and record audit events. These endpoints are not implemented.

## Error handling
Known business errors use HTTP status codes such as 401 unauthenticated, 404 missing resource, 409 conflict, and 400 validation. Do not return stack traces, SQL errors or secrets. Validation errors still need a unified production-grade response format.

## Database
Flyway migrations are under `backend/src/main/resources/db/migration`. Migration V1 creates users and hashed sessions; V2 creates the demo-credit ledger and game-session tables.
