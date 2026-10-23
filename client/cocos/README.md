# Cocos Creator native client

This is the native-client source for Cocos Creator 3.8.6. The API boundary, authentication, lobby, account summary, session history, logout, and three mini-game controller mechanics are implemented in TypeScript.

## Import and wire the native UI

1. Install Cocos Creator **3.8.6** and open this directory as a project.
2. Set the API origin on `AuthScreen.apiBaseUrl`. Use an HTTPS origin outside local development. The client does not persist bearer tokens to disk.
3. Create an Auth scene/root containing email and password EditBoxes, a status Label, Register and Sign In buttons. Attach `AuthScreen`; assign `emailInput`, `passwordInput`, `statusLabel`, `authRoot`, and `lobbyRoot` in the Inspector. Wire the buttons to `register` and `login`.
4. Create the lobby root with catalogue, status, profile, balance, score and history Labels plus Play, Finish and Logout buttons. Attach `LobbyScreen`; wire buttons to `refresh`, `playSelected`, `finishRound` and `logout`. Wire game-selection buttons to `selectGame` with one of `skyline-crash`, `neon-reels`, or `reef-ranger`.
5. For a dedicated game scene/panel, attach `MiniGameController`, choose a `gameId`, and assign its Labels and buttons. Wire Start to `startRound`, Finish to `finishRound`, the Skyline/Reef action button to `action`, and the four Neon Sequence tile buttons to `tile0Action` through `tile3Action`.
6. Set the intended start scene in Creator's Build panel. Preview and test sign-in, email-verification behavior, all three game types, network failures, session expiry, logout and orientation/resizing.
7. Build for each intended platform in Creator and run on physical Android/iOS devices before publishing.

## Game behavior and trust boundary

- **Skyline Crash**: 15-second tap challenge.
- **Neon Sequence**: memory sequence grows after correct rounds.
- **Reef Ranger**: tap-to-collect arcade challenge.
- Session creation/completion and the recorded score are server-controlled. Local taps are presentation-only and do not affect the saved server score or demo-credit balance.
- No payment SDK, deposits, withdrawals, cash-outs or redeemable credits are included.

## Type-check source

From the repository root:

```sh
npx --yes --package typescript@5.7.3 tsc -p client/cocos/tsconfig.ci.json
```

CI uses a minimal `cc` declaration shim for static TypeScript checking. That check is not a replacement for opening the project in Creator; the native editor import, rendering, platform builds and device tests must still be performed with the Creator toolchain.
