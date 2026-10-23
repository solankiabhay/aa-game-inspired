# Cocos Creator client scaffold

This directory now includes a Cocos Creator 3.8.6 project manifest, typed API/auth/game services, and basic scene-controller components. It is a **starter project scaffold, not a verified playable native app**: scene files, prefabs, final visual design, editor import, Android/iOS builds and physical-device tests still require the Creator editor/toolchain.

## Open and wire in Creator

1. Install Cocos Creator 3.8.6 (or update package.json and test against your selected supported version).
2. Open this directory as a project in Creator. Let the editor generate local settings/asset metadata as required.
3. Create a scene with an email EditBox, password EditBox, status Label and buttons. Attach AuthScreen to a node; assign the fields in the Inspector and wire buttons to register/login.
4. Create a lobby scene with catalogue/status Labels and buttons. Attach LobbyScreen, wire refresh and playSelected, and call refresh when the scene opens.
5. Configure AuthScreen.apiBaseUrl to your API origin. Use HTTPS outside local development. Configure the hosting/CORS policy if serving a web build from a separate origin.
6. Build and run through Creator on each intended platform, then test account registration/login, API errors, logout, session expiry and app background/resume.

## Security and scope

- Session tokens stay in memory and must never be logged or placed in URLs.
- Only non-redeemable demo credits are supported; no deposits, withdrawals, cash-outs or payment integrations.
- GameService currently starts and completes a simulated backend session. The controller is only a flow scaffold; original gameplay mechanics, animation, prefabs and UX polish are still required.
- No successful editor import, native build or device test is claimed until those actions are run in Cocos Creator.
