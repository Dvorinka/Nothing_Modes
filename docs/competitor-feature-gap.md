# Competitor feature-gap analysis — MacroDroid & Automate

Source: extracted on-device (Nothing Phone 3, A024) on 2026-11-01.
- MacroDroid `com.arlosoft.macrodroid`: 212 actions / 128 triggers / 87 constraints, enumerated from string-resource pairs (`X` + `X_help`).
- Automate `com.llamalab.automate`: 421 building blocks, enumerated from bundled `assets/en/help/block/*.html`.
- Ours: ~60 actions, ~20 triggers, ~35 conditions (incl. And/Or/Not).

## Structural lessons

| Their model | Ours | Gap |
|---|---|---|
| Variables everywhere — every block reads/writes named values | No variable system | **The** strategic gap. Most of their long tail (text manipulation, JSON parse, dialogs, pickers, compare values) is "variable plumbing" |
| If/Else, loops, fork/parallel fibers, subroutines, failure-catch | Group + conditions only | A "Run mode" action + per-action continue-on-error covers ~80% without a flowchart UI |
| Constraint logic AND/OR/XOR | And/Or/Not — parity since vc42 | Done |
| Plugin host (Locale/Tasker API) | None | Lets existing Tasker plugins run in our modes — real interop win |
| Default-assistant trigger | None | Long-press power → run a mode. Nobody else does this cleanly; differentiator |

## Tier 1 — cheap, high value

- **HTTP request action** (method/url/headers/body). Biggest single gap; enables Home Assistant, IFTTT, webhooks. Both competitors ship it.
- **Run shell command (Shizuku)** action. `PrivilegedShell` already exists — one catalog entry unlocks everything else for power users. Automate ships three variants (shell/privileged/root).
- **Run / enable / disable mode** action — modes calling modes (their subroutine + macro-enable).
- **Toast** and **Speak text (TTS)** actions — trivial feedback actions.
- **Headphones connected trigger** — already a condition; trivial to expose as trigger.
- **Shake / orientation / proximity triggers** — plain `SensorManager`.
- **App installed / removed trigger** — `PACKAGE_ADDED` broadcast.
- **Reject / end call** action — `TelecomManager.endCall()`/permission-gated.
- **Media track changed** trigger — extends the existing media-session listener.
- **Send broadcast / intent action** + **intent-received trigger** — interop primitives.
- **Sunrise/sunset** option on the time trigger.
- **Failed-login trigger** (photo/DND on wrong PIN) — `DeviceAdminReceiver`.

## Tier 2 — moderate

- **Variables** (set, compare, substitute into notification text/conditions). Gate behind "Advanced" — the moment this lands, most remaining MacroDroid actions become expressible.
- **Notification interact / snooze / reply** — deeper `NotificationListener` control beyond clear.
- **NFC tag scanned trigger** — Nothing phones have NFC; foreground dispatch or reader-mode.
- **Take photo / record audio** — camera + `MediaRecorder`.
- **Continue-on-error** flag per action (their failure-catch).
- **Ambient light / pressure / humidity / pedometer / significant-motion** sensors — conditions + triggers.
- **Activity recognition** (still/walk/run/drive) — needs Play-services permission, popular MacroDroid trigger.
- **Wi-Fi connect-to-SSID**, USB/BT tether toggles, preferred network type (5G/LTE) — all via Shizuku shell.
- **Locale/Tasker plugin host** — run Tasker plugins inside our modes.
- **Ask-before-run dialogs** (confirm/choice dialogs as actions).
- **QR generate / scan**, OCR (ML Kit), read/write file, screenshot-content checks.
- **Signal-strength / cell-tower** triggers and conditions (weak Wi-Fi → mobile data).
- **Doze/idle**, **work-profile quiet mode**, **AppOps set** — Shizuku one-liners.

## Tier 3 — defer or skip

- Full accessibility UI-interaction engine (click-by-coords, inspect tree, type into fields) — biggest scope, own milestone.
- Cloud storage (GDrive/OneDrive), FTP, local HTTP server, cloud messaging.
- Weather provider — possible via open-meteo (no API key) but adds a network dependency for a trigger.
- Foldable hinge, IR blaster, heart-rate, hotword, Wear OS/Pebble, CM profiles — N/A on Nothing hardware.
- Arrays/dictionaries/JSON manipulation — lands with variables.

## Reviewer Bluetooth ask — resolution

| Ask | Status |
|---|---|
| Disconnect from Bluetooth | Shipped — `DisconnectBluetoothDevice` (vc42, Shizuku) |
| Forget the Bluetooth | Shipped — `ForgetBluetoothDevice`/`removeBond` (vc42, Shizuku) |
| Do not connect to Bluetooth | **Covered by Forget** — Android has no public "blacklist but keep bond" API. Per-profile `setConnectionPolicy` via reflection is fragile across skins; revisit only if a concrete need survives unpairing |
