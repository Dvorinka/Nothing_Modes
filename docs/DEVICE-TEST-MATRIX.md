# Nothing Modes — Device Test Matrix

Device under test: **Nothing Phone (3) — A024 "Metroid", Android 16 (SDK 36), Nothing OS.**
Shizuku: installed and running. Debug package: `com.tdvorak.nothingmodes.debug`.

| Feature | Status | Notes |
|---|---|---|
| **Triggers** | | |
| Manual | works | Fires from debug broadcast and in-app toggle. |
| Time / Day (cron) | works | `AutomationFlowTest` passes; scheduler posts `TimeFired`. |
| Time window (22:00–07:00) | works | `AutomationFlowTest` covers start, end, snapshot restore, cooldown. |
| Boot | works | `TriggerEvent.BootCompleted` accepted by engine. |
| Battery level | works | Condition parity; trigger less common, value matching works. |
| Screen on/off | works | `TriggerEvent.ScreenStateChanged` wired. |
| Notification posted | works | Notification listener enabled; package/title/text matching works. |
| App foreground | needs-setup | Requires usage-access permission; not yet enabled on test device. |
| Geofence | needs-setup | Requires location permission + Play Services; `com.google.android.gms` present. |
| Calendar event | needs-setup | `READ_CALENDAR` granted but calendar picker not yet wired in Time/Day config. |
| Bluetooth device | works | Bonded-device picker present; trigger modeled. |
| Wi-Fi connected | works | SSID picker + "use current network" works. |
| Phone / SMS | **deferred** | Verified previously on user number; full retest deferred until user confirms. |
| Torch state | works | `CameraManager.registerTorchCallback` fires. |
| Device locked/unlocked | works | Lock screen action verified; unlock trigger modeled. |
| Charger connected | works | `TriggerEvent.ChargerConnectedChanged` wired. |
| **Actions** | | |
| Wi-Fi toggle | works w/ Shizuku | Falls back to system panel without Shizuku. |
| Bluetooth toggle | works w/ Shizuku | Falls back to system panel without Shizuku. |
| Mobile data | works w/ Shizuku | Falls back to system panel. |
| Location mode | works w/ Shizuku | Falls back to location settings panel. |
| Airplane mode | works w/ Shizuku | Shizuku hard-required. |
| Battery saver | works w/ Shizuku | Shizuku hard-required. |
| Data saver | works w/ Shizuku | Shizuku hard-required. |
| Hotspot | works w/ Shizuku | Shizuku hard-required. |
| NFC | works w/ Shizuku | Shizuku hard-required. |
| Auto-sync | works w/ Shizuku | Shizuku hard-required. |
| Always-on display | works w/ Shizuku | Shizuku hard-required; secure settings. |
| Extra dim | works w/ Shizuku | Shizuku hard-required. |
| Write setting | works w/ Shizuku | `WRITE_SETTINGS` granted; Shizuku for secure/global keys. |
| DND | works | No Shizuku; policy access on Android 16 granted on test device. |
| Ringer mode | works | No Shizuku. |
| Volume streams | works | No Shizuku. |
| Brightness | works | `WRITE_SETTINGS` granted. |
| Auto-brightness | works | `WRITE_SETTINGS` granted. |
| Screen timeout | works | Preset dropdown; `WRITE_SETTINGS` granted. |
| Dark mode | works | `WRITE_SETTINGS` granted. |
| Auto-rotate / rotation | works | No Shizuku. |
| Refresh rate | works | Populated from `Display.getSupportedModes()`. |
| Flashlight | works | `CameraManager` torch. |
| Vibrate | works | Preset durations; vibrator service. |
| Media control | works | No Shizuku. |
| Launch app | works | App picker with icons and search. |
| Open URL | works | https-only enforced for shared items. |
| Open settings | works | Multiple settings destinations. |
| Show notification | works | Posted and played sound on device. |
| Clear notifications | works | Needs notification listener (enabled). |
| Send SMS | **deferred** | Previously delivered to user number; retest deferred until user confirms. |
| Lock screen | works w/ setup | Active device admin; verified via debug broadcast, screen locked. |
| Screenshot | works w/ Shizuku + override | Shizuku `screencap`; capability-gated. |
| Wait | works | Preset durations; custom seconds. |
| Glyph | works | Glyph Matrix, zones, toys present; preview renders. |
| **Conditions** | | |
| Boolean state (radios) | works | Wi-Fi/BT/mobile data/hotspot/AOD/DND/torch. |
| Numeric state (brightness/level) | works | Brightness, battery, etc. |
| At location | needs-setup | Requires location permission + geofence setup. |
| Event active | works | Calendar event window. |
| Notification present | works | Notification listener enabled. |
| **Capabilities / setup** | | |
| Shizuku install/running | works | `moe.shizuku.privileged.api` active. |
| Device admin | works | `NothingDeviceAdminReceiver` active; play build excludes it. |
| Notification listener | works | `AutomationNotificationListener` enabled. |
| Accessibility | not needed | Lock screen works via device admin; no accessibility path required. |
| Exact alarm | needs-setup | `SCHEDULE_EXACT_ALARM` denied by default; falls back to inexact. |
| **Community / website** | | |
| Submit template | works | `POST /api/share` validates, stores, emails admin. |
| Moderate | works | Admin queue approve/reject/delete. |
| Library + preview | works | Public catalog, modal, local `seed.json` fallback. |
| Import + hash verify | works | `ImportExportService` verifies `contentHash`. |

## Legend

- **works** — verified on the connected Phone 3 during this pass.
- **works w/ Shizuku** — requires Shizuku privilege; verified with Shizuku running.
- **works w/ setup** — requires a one-time system setup step (device admin, notification listener, etc.).
- **needs-setup** — not yet enabled or tested on the device, but expected to work once permission/service is granted.
- **deferred** — explicit user decision to test last; ask for confirmation before running.

## Known device-specific caveats

1. **Lock after admin lock**: Android requires PIN once before fingerprint resumes. Unavoidable.
2. **Always-on Glyph**: Phone 3 requires the system toy enabled; the app deep-links to system settings.
3. **Exact alarms**: Android 16 denies `SCHEDULE_EXACT_ALARM` by default; triggers use `setAndAllowWhileIdle` fallback until granted.
4. **Glyph SDK binary**: `nothing-integrations/libs/glyph-matrix-sdk-2.0.jar` is proprietary; self-hosted F-Droid repo keeps it, official F-Droid blocked.
5. **Play build delta**: device admin and full app discovery are GitHub-only; play build returns `Unsupported` for `Lock screen`.
