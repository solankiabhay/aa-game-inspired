# Cocos Creator integration scaffold

This folder contains typed API/auth/game service modules intended for a Cocos Creator 3.x project. It is **not yet a complete importable Creator project**: editor-generated project settings, scene assets, prefabs, native builds and device testing remain to be done.

## Source modules
- `assets/scripts/ApiClient.ts`: XHR wrapper with 10-second timeout, JSON parsing, error mapping and in-memory Bearer token.
- `assets/scripts/AuthService.ts`: registration, login, profile, demo-credit balance and logout.
- `assets/scripts/GameService.ts`: enabled catalogue, session start/complete and recent history.

## Integration notes
1. Create a Cocos Creator 3.x project in the Creator editor and copy these TypeScript files under its `assets/scripts` directory.
2. At boot, call `ApiClient.configure("https://your-api-host")` for the deployed API. The default `http://localhost:8080` is for local development only.
3. Build scenes/components around the service methods; show API errors and loading states in the UI.
4. Keep session tokens in memory; never log them or place them in URLs.
5. Mobile deployments require HTTPS. Test on actual devices and configure any platform-specific network permissions in Creator.
6. The backend currently implements simulated demo scores only; credits have no monetary value and are not consumed by game sessions.

No claim of a successful Cocos build is made until a complete project is opened and built with the selected Creator editor version.
