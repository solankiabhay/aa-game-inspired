# Production deployment scaffold

This directory contains a Caddy TLS reverse proxy configuration. Production stack definition is at repository root in `docker-compose.prod.yml`.

Before starting:
1. Provision a Linux host with Docker Engine and Compose plugin; point the domain's A/AAAA records to it.
2. Copy `deploy/env.prod.example` to `.env.prod`, then replace every placeholder with `APP_DOMAIN`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, and `MYSQL_ROOT_PASSWORD`, and `REDIS_PASSWORD`. Use unique generated secrets; never commit `.env.prod`.
3. Review firewall rules: expose only TCP 80/443 (and optional UDP 443); do not expose MySQL or the API directly.
4. Start with `docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build`.
5. Verify TLS, `https://YOUR_DOMAIN/api/health`, authentication, admin authorization, and game-session behavior.
6. Test backup and restore on staging before production traffic.

This is a deployment template, not a deployed service. It does not configure DNS, cloud firewall, host patching, external secret manager, monitoring/alerting, or a tested disaster-recovery SLA. The SMTP values must belong to a real provider; verify email delivery and link routing in staging. Validate TLS and database TLS settings against your managed/self-hosted MySQL certificate setup before using this stack. Keep backups off-host and encrypt them at rest.
