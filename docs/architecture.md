# Architecture

- `client/`: Cocos Creator client (virtual/demo gameplay only).
- `backend/`: Java 21 + Spring Boot REST API.
- MySQL: local development persistence via Docker Compose.
- `admin/`: planned administration UI.

## Safety and scope

This project is an original educational demo-game implementation. It uses virtual credits only and does not implement real-money wagering, deposits, withdrawals, or cash-outs. Never commit secrets or private payment QR codes to the repository or client bundle.

## Initial API

- `GET /api/health` returns service health.
