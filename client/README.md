# Client applications

## Working browser MVP
A lightweight browser client is implemented at `client/web/index.html` and served by Spring Boot at `http://localhost:8080/` after the backend is running. It calls the real API for registration, login/logout, game catalogue, demo-credit balance/ledger, session start and server-generated completion score. The canonical source is `client/web/index.html`; the backend static copy is `backend/src/main/resources/static/index.html`.

## Cocos Creator client — scaffold started, project still pending
Typed API, auth and game service modules are available in [the Cocos scaffold](cocos/README.md). A full TypeScript/Cocos Creator native/mobile project is not yet implemented. It requires selecting/pinning the Creator editor version and adding valid project metadata/assets.

### Planned scenes
- Boot / loading
- Authentication
- Lobby
- Crash demo
- Reels demo
- Fishing demo
- Profile and demo-credit history

## Client responsibilities
Render UI and animation, collect user input, call the API, display server-returned state and handle errors/reconnects. The client must never be the authority for account permissions, demo-credit balance or game results.

## Remaining Cocos checklist
- [ ] Pin Cocos Creator version and add valid project settings.
- [ ] Add design tokens and reusable buttons/cards/forms.
- [ ] Add responsive layout and safe-area handling.
- [ ] Add typed API client and request timeout/error handling.
- [ ] Port auth and lobby flow to Creator scenes.
- [ ] Build three original playable Creator game scenes.
- [ ] Add device and aspect-ratio testing.
- [ ] Confirm all credits are visibly marked as simulated.
