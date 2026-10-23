# Data Model and Migration Plan

Flyway migrations are implemented under `backend/src/main/resources/db/migration`. Timestamps are stored in UTC-compatible database timestamps. Public session IDs are UUIDs; authentication uses opaque random tokens whose SHA-256 hashes are stored.

## Implemented tables

- `app_users` (V1): auto-increment ID, unique normalized email, BCrypt password hash, role, status, created timestamp.
- `user_sessions` (V1): SHA-256 token hash, user ID, expiry and created timestamp; foreign key cascades on user deletion.
- `demo_credit_accounts` (V2): one row per user and non-negative integer balance. New accounts start at 1,000 non-redeemable credits.
- `demo_credit_ledger` (V2): append-by-application-logic entries containing delta, balance-after, reason, reference and timestamp.
- `game_sessions` (V2): UUID, owner, game ID, status, score, created/finished timestamps.
- `audit_events` (V3): actor, action, target, required reason, before/after values and timestamp.
- `game_configs` (V4): game ID, display title, category, description, enabled flag, updater and update timestamp.

## Invariants
1. Email is normalized to lowercase and unique.
2. Passwords are hashed; plaintext is never persisted.
3. Tokens are random and only token hashes are persisted; the token itself is returned once at login.
4. User creation, welcome-credit account and welcome-ledger entry are in one transaction.
5. Demo-credit balance is not redeemable, transferable or wagered in this MVP.
6. The server generates demo scores; the client cannot submit a score/result.
7. A game session can be completed only once and only by its owner.
8. Admin user-status and game-config mutations require an ADMIN session, a reason and an audit event.
9. Suspended users cannot log in or use sessions; suspension revokes existing sessions.

## Known gaps
- The ledger is not yet used for credit/debit gameplay transactions, idempotency keys or concurrent wallet mutations because demo credits are not spent.
- Admin role promotion is a controlled manual database operation described in `ADMIN_BOOTSTRAP.md`; it is not a public API.
- Manual role changes must be captured in the operator audit log; the application audit table records API mutations.
- Full retention/deletion policy, operational backup strategy and production privacy review remain pending.

## Indexes
Migrations include indexes for session lookup/expiry, user ledger history, user game-session history and audit event time/target. Review query plans with realistic production-like data before launch.
