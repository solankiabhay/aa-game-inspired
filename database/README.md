# Database

MySQL runs locally through the root `docker-compose.yml`. Versioned database migrations are not added yet.

## Rules for schema changes
- Add Flyway migrations under `backend/src/main/resources/db/migration`.
- Never edit a migration that has already been applied in a shared environment; add a new migration.
- Use constraints to enforce normalized-email uniqueness and idempotency.
- Use transactions for balance + ledger writes.
- Store UTC timestamps.
- Use synthetic test data only.
- Document backup and restore procedures before staging deployment.

See [data model](../docs/DATA_MODEL.md) for proposed tables and invariants.
