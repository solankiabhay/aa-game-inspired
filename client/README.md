# Cocos Creator Client

This folder is reserved for the original TypeScript/Cocos Creator client. A full Cocos project requires the Creator editor and its project metadata/assets; do not treat this README as a runnable game project.

## Planned scenes
- Boot / loading
- Authentication
- Lobby
- Crash demo
- Slots demo
- Fishing demo
- Profile and demo-credit history

## Client responsibilities
Render UI and animation, collect user input, call the API, display server-returned state and handle errors/reconnects. The client must never be the authority for account permissions, demo-credit balance, game results or payout calculation.

## Prototype checklist
- [ ] Pin Cocos Creator version and add valid project settings.
- [ ] Add design tokens and reusable buttons/cards/forms.
- [ ] Add responsive layout and safe-area handling.
- [ ] Add typed API client and request timeout/error handling.
- [ ] Add login/register forms with loading/validation states.
- [ ] Add lobby and original game scenes.
- [ ] Add device and aspect-ratio testing.
- [ ] Confirm all credits are visibly marked as simulated.
