# Local Deployment and Operations

## One-command local stack
Prerequisites: Docker Engine and Docker Compose v2.

1. Clone the repository and enter its root.
2. Copy `.env.example` to `.env`; use only local development values.
3. Run `docker compose up --build -d`.
4. Wait for MySQL health and the API container to start.
5. Open:
   - Browser client: `http://localhost:8080/`
   - Admin console: `http://localhost:8080/admin/index.html`
   - Health: `http://localhost:8080/api/health`
   - Game catalogue: `http://localhost:8080/api/games`

The API container runs Flyway migrations on startup. The first admin account must be created and promoted through the controlled steps in [ADMIN_BOOTSTRAP.md](ADMIN_BOOTSTRAP.md). Do not make the database publicly accessible.

## Troubleshooting
- `docker compose ps` — verify containers and health.
- `docker compose logs -f mysql` — inspect database startup.
- `docker compose logs -f api` — inspect application startup/migration errors.
- If you change MySQL credentials after the data volume has been initialized, the existing database users are not automatically recreated. Reset only a disposable local database with `docker compose down -v`; this permanently deletes local database data.
- `docker compose down` stops containers but keeps the database volume.

## Production deployment gate
Do not deploy this prototype directly to the public internet. Before production, add TLS/reverse proxy and security headers, secrets management, login/registration rate limiting, email verification/password recovery, unified errors, DB least-privilege users and network restrictions, automated backup/restore validation, dependency/image scanning, monitoring/alerting, load testing, privacy/legal review and a documented rollback plan. Configure resource limits and durable storage for the target platform.
