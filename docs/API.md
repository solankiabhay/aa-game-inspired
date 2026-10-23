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
- GET `/api/games/sessions?limit=20` — lists the authenticated user’s latest sessions (limit capped at 100).
- GET `/api/games/sessions/{sessionId}` — retrieves a session belonging to the authenticated user.

These are demo simulation endpoints, not production game mechanics. Demo credits are not wagered, transferred, sold or redeemable.

### Admin
- GET `/api/admin/users?query=...` — returns at most 100 matching users.
- PATCH `/api/admin/users/{userId}/status` — sets status to `ACTIVE` or `SUSPENDED`, requires a reason, revokes sessions on suspension, and writes an audit event.
- GET `/api/admin/audit-events` — latest 100 audit events.
- GET `/api/admin/games` — list configured games.
- PATCH `/api/admin/games/{gameId}/config` — update title, description and enabled status; requires a reason and creates an audit event.

Admin endpoints require a valid session for a database-provisioned `ADMIN` role. There is deliberately no public endpoint to grant admin privileges. Bootstrap the first administrator manually using the restricted database runbook; never expose a default admin password in source code.

## Planned hardening
- Rate limiting and abuse controls for registration/login.
- Email verification, password reset, refresh tokens, and stronger account recovery.
- Standardized validation/malformed-JSON error envelopes are implemented; complete coverage for all unexpected exceptions remains.
- Admin pagination, richer confirmation dialogs, MFA/SSO and production accessibility review.

## Database
Flyway migrations are under `backend/src/main/resources/db/migration`: V1 users/sessions, V2 demo-credit ledger/game sessions, V3 admin audit events.
