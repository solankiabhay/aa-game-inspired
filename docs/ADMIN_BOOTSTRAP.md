# Administrator Bootstrap Runbook

There is intentionally no public API to promote a user to ADMIN. The first administrator must be provisioned by an operator with restricted database access.

## Before you begin
- Use a private, non-production environment first and verify the process.
- Back up the database and ensure only authorized operators can connect.
- Register the intended admin account through `POST /api/auth/register` and verify its email through your organization’s process (email verification is not implemented in this MVP).
- Use a unique account protected by a long password. Never reuse a personal password.

## Promote the account
Connect using your normal secured MySQL administration channel and execute the following with the exact registered email. Do not put database passwords in shell history or commit them.

```sql
START TRANSACTION;
SELECT id, email, role, status
FROM app_users
WHERE email = LOWER('admin@example.com')
FOR UPDATE;

UPDATE app_users
SET role = 'ADMIN'
WHERE email = LOWER('admin@example.com')
  AND status = 'ACTIVE';

COMMIT;
```

Replace `admin@example.com` with the intended account. Confirm exactly one row was updated and then authenticate through `POST /api/auth/login`. Verify `GET /api/admin/users` returns HTTP 200. A normal user should receive HTTP 403.

## Revoke admin access
Set the account role back to `USER`, revoke all sessions for the account, and record the action through your operational audit process:

```sql
START TRANSACTION;
UPDATE app_users SET role = 'USER' WHERE email = LOWER('admin@example.com');
DELETE FROM user_sessions
WHERE user_id = (SELECT id FROM app_users WHERE email = LOWER('admin@example.com'));
COMMIT;
```

## Important limitations
The current admin audit table records user status changes through the API; manual role changes above are not automatically written to that table. Record bootstrap/revocation in your controlled operational audit log. Production should use managed operator identity, approval, stronger audit guarantees and a reviewed migration/runbook.
