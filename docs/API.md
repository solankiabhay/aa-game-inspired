# REST API Contract

Base path: /api. JSON request/response bodies. Use HTTPS outside local development.

## Implemented

### GET /api/health
Returns {"status":"UP","service":"aa-game-api"}. Process health only; not a database readiness check.

### GET /api/games
Returns the static public catalog of three original demo-only games. Catalogue is currently in memory, not database-configurable.

### Authentication and user data
- POST /api/auth/register — validates email and a 12–72 character password, stores a BCrypt password hash, creates a user, and atomically grants 1,000 non-redeemable demo credits with a ledger entry. When email verification is enabled, sends a one-time verification link and blocks login until verified.
- POST /api/auth/verify-email — consumes a single-use, 24-hour verification token sent in the email.
- POST /api/auth/login — returns an opaque 256-bit random Bearer token with a 24-hour expiry. Only its SHA-256 hash is stored in the database.
- POST /api/auth/password-reset/request — returns the same generic 202 response for existing and unknown accounts; when enabled, sends a single-use reset link.
- POST /api/auth/password-reset/confirm — consumes a single-use, 30-minute token, changes the BCrypt password, and revokes all existing sessions for the account.
- GET /api/auth/me — authenticated profile.
- POST /api/auth/logout — revokes the presented session token.
- GET /api/auth/me/demo-credits — authenticated demo-credit balance and redeemable:false.
- GET /api/auth/me/ledger — latest 100 demo-credit ledger entries.

Email verification and password reset are disabled by default for local development and enabled in the production Compose template. Configure SMTP credentials, a public HTTPS base URL and a sender address before enabling them.

Example registration:
```json
{"email":"player@example.com","password":"StrongPassword123!"}
```
Example login response:
```json
{"accessToken":"opaque-token","tokenType":"Bearer","expiresAt":"ISO-8601 timestamp"}
```
Send the returned token as Authorization: Bearer <token>. Never log passwords, action tokens or session tokens.

### Game sessions
- POST /api/games/{gameId}/sessions — authenticated session start for skyline-crash, neon-reels, or reef-ranger.
- POST /api/games/sessions/{sessionId}/complete — completes an active session; score is generated server-side and cannot be chosen by the client. A completed session cannot be completed again.
- GET /api/games/sessions?limit=20 — lists the authenticated user’s latest sessions (limit capped at 100).
- GET /api/games/sessions/{sessionId} — retrieves a session belonging to the authenticated user.

These are demo simulation endpoints, not production game mechanics. Demo credits are not wagered, transferred, sold or redeemable.

### Admin
- GET /api/admin/users?query=... — returns at most 100 matching users.
- PATCH /api/admin/users/{userId}/status — sets status to ACTIVE or SUSPENDED, requires a reason, revokes sessions on suspension, and writes an audit event.
- GET /api/admin/audit-events — latest 100 audit events.
- GET /api/admin/games — list configured games.
- PATCH /api/admin/games/{gameId}/config — update title, description and enabled status; requires a reason and creates an audit event.

Admin endpoints require a valid session for a database-provisioned ADMIN role. There is deliberately no public endpoint to grant admin privileges. Bootstrap the first administrator manually using the restricted database runbook; never expose a default admin password in source code.

## Remaining hardening
- MFA, refresh-token rotation, and stronger account recovery protections.
- Rate limiting is in-process locally and Redis-backed in the production template; production reverse-proxy IP trust must be verified.
- Standardized validation/malformed-JSON error envelopes are implemented; complete coverage for unexpected exceptions remains.
- Admin pagination, richer confirmation dialogs, MFA/SSO and production accessibility review.

## Database
Flyway migrations are under backend/src/main/resources/db/migration: V1 users/sessions, V2 demo-credit ledger/game sessions, V3 admin audit events, V4 game configuration, V5 email verification and one-time action tokens.
