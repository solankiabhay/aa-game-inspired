# Security Checklist and Threat Model

## Assets to protect
Account credentials, session tokens, email addresses, virtual-credit integrity, admin capabilities, source repository, deployment secrets and logs.

## Main threats and controls
- **Credential stuffing/brute force:** rate-limit login and registration, use safe password hashing, monitor repeated failures, generic auth responses.
- **Broken access control:** deny by default; authorize every protected request on the server; test player/admin boundary cases.
- **Client tampering:** treat every client field as untrusted; calculate balance and game results on server.
- **Replay/double submission:** idempotency keys, unique constraints and transactional writes.
- **SQL injection:** parameterized ORM/repository queries; never concatenate untrusted input into SQL.
- **XSS/CSRF/token theft:** choose browser/mobile token storage deliberately; apply CSP and appropriate cookie flags where cookies are used; validate CORS allowlists; avoid tokens in URLs.
- **Secret leakage:** environment/secret manager, secret scanning, no private keys, credentials, production URLs with tokens or private QR images in APK/repository.
- **Admin abuse:** least privilege, MFA for real admin operations, audit events, explicit confirmations and restricted admin accounts.
- **Dependency vulnerabilities:** pinned versions, dependency updates and automated scanning.
- **Denial of service:** request/body limits, rate limits, timeouts, sensible pagination, capacity/load tests.
- **Sensitive logs:** redact credentials and personal data; define access and retention.
- **Database compromise:** least-privileged DB account, private network, TLS where applicable, tested backups and restore procedure.

## Required before release
- [ ] Threat model review and abuse-case tests.
- [ ] Password hash algorithm and parameters reviewed.
- [ ] Session expiration, refresh rotation/revocation, logout behavior specified.
- [ ] Server-side authorization tested for every admin endpoint.
- [ ] Secrets and dependency scans pass.
- [ ] CORS, TLS, security headers and rate limits configured for target environment.
- [ ] Database backup/restore rehearsal succeeds.
- [ ] Privacy notice, retention and account deletion process defined.
- [ ] Legal review confirms intended distribution and demo-game design.
- [ ] No real-money wagering, deposits, withdrawals or cash-out code in this prototype.

Security controls are design requirements until code and tests prove they exist.
