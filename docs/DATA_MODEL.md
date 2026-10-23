# Data Model and Migration Plan

Use Flyway versioned migrations once persistence is implemented. Store UTC timestamps. Use UUIDs or opaque IDs for public references; never expose sequential database IDs as authorization.

## Proposed tables
- `users`: id, normalized_email (unique), password_hash, role, status, created_at, updated_at.
- `sessions` or `refresh_tokens`: user_id, token_hash, expires_at, revoked_at, created_at, device metadata kept minimal.
- `demo_credit_accounts`: user_id unique, balance (integer smallest virtual-credit unit), version, updated_at.
- `demo_credit_ledger`: id, user_id, amount_delta, reason_code, reference_type, reference_id, idempotency_key, created_at. Unique constraints should prevent duplicate application of a transaction.
- `game_sessions`: id, user_id, game_code, status, rules_version, request_key, started_at, completed_at.
- `game_results`: session_id unique, result_payload, created_at; payload must exclude secrets.
- `game_configs`: game_code, version, enabled, config_json, updated_by, updated_at.
- `audit_events`: actor_user_id, action, target_type, target_id, reason, before_json, after_json, request_id, created_at.

## Invariants
1. Normalized email is unique.
2. Password hashes only; never plaintext or reversible encryption.
3. Ledger is append-only from application logic. Corrections use compensating entries, not row edits.
4. A balance update and corresponding ledger row happen in one transaction.
5. The server rejects a debit that would make balance negative.
6. Each idempotency key is scoped to the user/operation and unique in its required scope.
7. Game result and associated ledger mutation commit atomically.
8. Admin changes create an audit record.
9. Do not store full IP/device fingerprints unless there is a documented need, retention policy and privacy review.

## Indexes
Add indexes for normalized_email, ledger(user_id, created_at), game_sessions(user_id, started_at), audit_events(created_at), and idempotency uniqueness. Validate query plans with realistic data.

## Migration strategy
- V1 users and role/status enums or constrained strings.
- V2 sessions/refresh token hashes.
- V3 demo-credit account and ledger.
- V4 game catalog/session/result tables.
- V5 admin configuration and audit events.

Names and exact types should be finalized in migrations and then reflected in this document.
