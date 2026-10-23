# Admin Dashboard

## Working dashboard
A lightweight API-connected admin console is implemented at `admin/index.html` and served by Spring Boot at `http://localhost:8080/admin/index.html`. The same-origin copy is `backend/src/main/resources/static/admin/index.html`.

Features:
- Administrator sign-in using the existing backend session endpoint.
- Search users by email.
- Suspend/reactivate user accounts with a required reason.\n- Edit game title/description and enable/disable demo entries with audited reasons.
- View recent audit events.
- In-memory-only browser token; sign out calls session revocation.
- Permission enforcement occurs in the backend, not just in the UI.

## First administrator
There is no public admin-promotion endpoint by design. Follow [the controlled bootstrap runbook](../docs/ADMIN_BOOTSTRAP.md) to provision the first ADMIN role directly in the database.

## Remaining production work
- Replace basic HTML UI with a versioned, tested component framework if desired.
- Add pagination, stronger confirmation dialogs, filters and accessibility testing.
- Add pagination, richer confirmation dialogs and stronger admin MFA/SSO.
- Add stronger admin MFA/SSO, approval controls and operational audit for role changes.
- Perform security review before any public deployment.

Never include real-money payment, deposit or withdrawal tools in this prototype.
