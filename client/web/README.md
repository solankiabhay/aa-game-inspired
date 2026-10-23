# Browser client

Arcade Lab is a responsive, dependency-free browser client served by Spring Boot from `backend/src/main/resources/static/index.html`. The editable source is `client/web/index.html`; keep the two copies in sync when making client changes.

## Features

- Responsive arcade landing page, game catalogue cards and account dashboard.
- Email/password registration and login, including the email-verification-required path.
- Authenticated profile, demo-credit balance, ledger and recent game sessions.
- Interactive browser mini-challenges for Skyline Crash, Neon Sequence and Reef Ranger.
- Server-tracked sessions and server-generated scores; client activity does not determine the saved score.
- One-time email verification and password-reset pages.
- In-memory Bearer token only; no localStorage/sessionStorage token persistence.
- Keyboard focus styling, semantic forms, status announcements and reduced-motion support.

## Run locally

1. Start the backend from the repository's documented local setup.
2. Open `http://localhost:8080/`. The page uses same-origin `/api` endpoints.
3. Create a demo account or sign in. If email verification is enabled, configure SMTP and verify the account before login.
4. Choose a game, start a server-tracked round, play the mini-challenge and finish the round to save the server-generated score.

Serving the static page from a separate origin is not the default. If you do so, configure a specific HTTPS API origin and restrictive CORS policy; do not use wildcard credentialed CORS.

## Validate

Run `node client/web/test-client.mjs` from the repository root. CI runs this check automatically. It validates the inline JavaScript syntax, duplicate HTML IDs, literal DOM lookups, key API routes, and presence of all three game integrations. This is a static smoke test, not a substitute for cross-browser end-to-end testing.

## Deployment notes

- Deploy behind the repository's HTTPS reverse-proxy setup and serve the client and API on the same origin.
- Configure real SMTP credentials before enabling verification/password recovery.
- Test login, registration, expired sessions, rate limits, mobile layouts and screen-reader behavior in staging.
- No payments, deposits, wagering, cash-outs or redeemable credits are implemented.
- This browser client is distinct from the Cocos Creator native project. The native project's editor import and Android/iOS builds are not yet verified.
