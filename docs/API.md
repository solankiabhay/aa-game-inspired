# REST API Contract Plan

Base path: `/api`. JSON request/response bodies. Use HTTPS outside local development. This document describes the intended contract; endpoints marked planned do not exist until implemented.

## Implemented
### GET /api/health
Response:
```json
{"status":"UP","service":"aa-game-api"}
```
This is a basic process health response, not yet a database readiness check.

## Planned authentication
- POST `/api/auth/register` — email, password, password confirmation or client-side confirmation; server validates email/password and creates account.
- POST `/api/auth/login` — email/password; returns session/access credentials according to chosen auth design.
- POST `/api/auth/refresh` — rotates refresh credential if refresh-token sessions are selected.
- POST `/api/auth/logout` — revokes current refresh/session token.
- POST `/api/auth/password/forgot` — always returns a generic response to reduce account enumeration.
- POST `/api/auth/password/reset` — validates a one-time, expiring reset token.

Never log passwords, access tokens, refresh tokens or reset tokens.

## Planned user
- GET `/api/me` — current user profile.
- PATCH `/api/me` — only explicitly allowed profile fields.
- GET `/api/me/demo-credits` — current demo-credit balance.
- GET `/api/me/ledger?cursor=...` — paginated ledger history.

## Planned game
- GET `/api/games` — enabled demo game catalog.
- POST `/api/games/{gameId}/sessions` — start a session; requires idempotency key.
- POST `/api/games/sessions/{sessionId}/action` — submit an allowed action for that game state.
- GET `/api/games/sessions/{sessionId}` — retrieve authoritative session state/result.

Game-specific validation must reject illegal state transitions and out-of-range inputs. The client cannot choose a result, payout or final balance.

## Planned admin (role required)
- GET `/api/admin/users?query=...`
- PATCH `/api/admin/users/{userId}/status`
- GET/PATCH `/api/admin/games` and `/api/admin/games/{gameId}/config`
- GET `/api/admin/audit-events`

Admin mutations require authorization, validation, an audit event and a reason where appropriate.

## Error format
Use a stable structure such as:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Please check the submitted fields.",
  "requestId": "correlation-id",
  "fieldErrors": [{"field":"email","code":"INVALID_FORMAT"}]
}
```
Do not return stack traces, SQL errors, internal class names or secrets. Use appropriate HTTP status codes (400 validation, 401 unauthenticated, 403 forbidden, 404 absent resource, 409 conflict, 429 rate limited, 5xx unexpected server failure).

## API implementation checklist
- DTOs and Bean Validation
- central exception handler
- consistent request IDs
- authentication and role enforcement
- pagination limits
- idempotency for mutation endpoints
- OpenAPI spec and integration tests after initial controllers are implemented
