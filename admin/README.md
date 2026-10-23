# Admin Dashboard

The admin UI is planned; no working dashboard is implemented yet.

## Recommended first cut
Use a pinned TypeScript web stack (for example React + Vite) after agreeing on the framework. Implement:
- Sign-in/session handling
- Overview cards for users, enabled demos and recent audit events
- User search and account status controls
- Demo game configuration editor with schema validation
- Audit event table with pagination and filters
- Loading, empty, error and permission-denied states

## Security requirements
- Hiding a route is not authorization; API must enforce admin roles.
- Do not store secrets in frontend environment variables.
- Require reason/confirmation for administrative changes.
- Display audit history and actor identity for each sensitive change.
- Never include real-money payment, deposit or withdrawal tools in this prototype.

## Status
Documentation only. Choose framework, add package manifest, implement components, tests and build pipeline before claiming it is functional.
