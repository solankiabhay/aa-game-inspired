# Product Requirements and Prototype Screens

## Product objective
Deliver an original mobile-first demo-game application. Prioritize clear navigation, fast feedback, understandable rules, accessible forms, and transparent simulated-credit messaging.

## Personas
- Guest: can view the lobby and demo information.
- Registered player: can sign in, play demos, see profile and virtual-credit history.
- Administrator: can manage demo configuration and account status through a separate protected web console.
- Operator/developer: monitors health, logs, test results and releases.

## Navigation map
1. Splash / loading
2. Welcome: Login, Create account, Demo mode explanation
3. Registration: email, password, confirm password, terms acknowledgement
4. Login: email/password, show/hide password, forgot password
5. Lobby: balance chip marked “Demo Credits”, game cards, recent activity
6. Crash demo: round state, multiplier display, start/stop controls, rules/help
7. Slots demo: spin control, fixed demo stake options, result animation, rules
8. Fishing demo: aim/shoot interaction, targets, timer, virtual score
9. Wallet/history: ledger entries only; no deposit or withdrawal flows
10. Profile/settings: email, sign out, privacy/help
11. Admin: overview, users, game configuration, audit events

## UX principles
- Every screen has loading, empty, success and recoverable error states.
- Disable duplicate submissions while a request is pending.
- Use clear confirmation for consequential admin changes.
- Do not present simulated outcomes as financial earnings.
- Keep tap targets comfortable, support common mobile aspect ratios, and test on small screens.
- Do not include third-party logos, artwork, copy or exact screen designs.

## Prototype acceptance criteria
- A guest can navigate the lobby and open each demo.
- Registration/login errors are actionable but do not reveal whether an arbitrary email is registered.
- Client never sets the authoritative balance or game result.
- All virtual-credit changes have a server-side ledger record.
- A refresh/reconnect does not duplicate a completed credit transaction.
- Admin endpoints reject non-admin users even if the UI is bypassed.
- Demo label and “no monetary value” explanation remain visible near credit balance and game entry points.

## Visual direction
Use an original dark navy base with warm amber highlights, crisp typography, restrained motion and reusable components. Avoid copying AA Game branding or exact layouts. Keep colors, spacing and typography as design tokens rather than hard-coded per-screen values.

## Out of scope
Real-money functionality, payments, withdrawals, referral bonuses with monetary value, production marketing claims, and remote loading of unreviewed executable assets.
