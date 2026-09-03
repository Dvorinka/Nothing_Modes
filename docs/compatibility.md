# Compatibility Matrix

## Primary Target

| Property | Value |
|---|---|
| Device | Nothing Phone (3) |
| Identifier | `Glyph.DEVICE_23112` |
| Android | 16 (API 36) / 17 (API 37) |
| Nothing OS | 4.1 / 5.0 |
| Glyph | Light stripe (via Glyph SDK) |
| Glyph Matrix | 25x25, with Glyph Touch |
| Matrix system version | 20250801+ for `setAppMatrixFrame` |

## Device Matrix

| Device | Identifier | Android | Glyph | Glyph Matrix | DND | Extra Dim | Brightness | Shizuku | Notes |
|---|---|---|---|---|---|---|---|---|---|
| Phone (3) | `DEVICE_23112` | 16-17 | Yes | 25x25 + Touch | Yes | Yes | Yes | Yes | Reference device |
| Phone (4a) Pro | `DEVICE_25111p` | 16+ | No | 13x13, AOD only | Yes | Yes | Yes | Yes | No Glyph Touch, AOD toys only |
| Phone (4a) | `DEVICE_25111` / `is25111()` | 14+ | Yes (6 LEDs) | No | Yes | Yes | Yes | Yes | Light stripe only |
| Phone (4b) | `DEVICE_25131` / `is25131()` | 14+ | Yes (4 LEDs) | No | Yes | Yes | Yes | Yes | Light stripe only |
| Phone (3a) / (3a) Pro | `DEVICE_24111` / `is24111()` | 14+ | Yes (36 LEDs) | No | Yes | Yes | Yes | Yes | Light stripe only |
| Phone (2a) Plus | `DEVICE_23113` / `is23113()` | 14+ | Yes (26 LEDs) | No | Yes | Yes | Yes | Yes | Light stripe only |
| Phone (2a) | `DEVICE_23111` / `is23111()` | 14+ | Yes (26 LEDs) | No | Yes | Yes | Yes | Yes | Light stripe only |
| Phone (2) | `DEVICE_22111` / `is22111()` | 12+ | Yes (33 LEDs) | No | Yes | Yes | Yes | Yes | End of major OS support |
| Phone (1) | `DEVICE_20111` / `is20111()` | 12+ | Yes (14 LEDs) | No | Yes | Yes | Yes | Yes | End of major OS support |

## Capability Status Legend

- **Yes** — Supported via public Android API or Nothing SDK
- **Shizuku** — Requires Shizuku when public API is insufficient
- **No** — Unsupported on this device

## Feature Capability Details

### DND (Do Not Disturb)

- **API:** `NotificationManager.setInterruptionFilter()` / `setNotificationPolicy()`
- **Permission:** `ACCESS_NOTIFICATION_POLICY` (granted via system settings)
- **All devices:** Yes (standard Android API)

### Extra Dim

- **API:** `Settings.Secure.reduce_bright_colors_activated` (0/1)
- **Intensity:** `Settings.Secure.reduce_bright_colors_level` (0-100)
- **Permission:** `WRITE_SECURE_SETTINGS` (not granted to normal apps)
- **Fallback:** Shizuku `settings put secure reduce_bright_colors_activated 1`
- **Android 16 change:** Being integrated into brightness slider on some Pixel devices. Nothing devices may keep the toggle.
- **All Nothing devices:** Yes (via Shizuku) or limited (public API may not work without WRITE_SECURE_SETTINGS)

### Brightness

- **API:** `Settings.System.screen_brightness` (0-255)
- **Auto:** `Settings.System.screen_brightness_mode` (0=manual, 1=automatic)
- **Permission:** `WRITE_SETTINGS` (granted via system settings)
- **All devices:** Yes

### Screen Timeout

- **API:** `Settings.System.screen_off_timeout` (milliseconds)
- **Permission:** `WRITE_SETTINGS`
- **All devices:** Yes

### Dark Mode

- **API:** `UiModeManager.setNightMode()` (API 31+ — may be restricted to system apps)
- **Fallback:** Shizuku `cmd uimode night no|yes|auto`
- **All devices:** Yes (via Shizuku) or limited (UiModeManager may reject non-system apps)

### Volume

- **API:** `AudioManager.setStreamVolume()` for MEDIA, RING, ALARM, NOTIFICATION
- **Permission:** None for media/alarm. DND policy access for RING/NOTIFICATION to 0.
- **All devices:** Yes

### Glyph (Light Stripe)

- **API:** Nothing Glyph SDK (`com.nothing.ketchum.GlyphManager`)
- **Permission:** `com.nothing.ketchum.permission.ENABLE`
- **Min Android:** 14 (API 34)
- **Devices:** Phone (1), (2), (2a), (2a) Plus, (3a), (3a) Pro, (4a), (4b)
- **NOT on:** Phone (3) — uses Glyph Matrix instead (but SDK is unified)

### Glyph Matrix

- **API:** Nothing Glyph Matrix SDK (`com.nothing.ketchum.GlyphMatrixManager`)
- **Permission:** `com.nothing.ketchum.permission.ENABLE`
- **Min Android:** 14 (API 34)
- **Devices:** Phone (3) (25x25 + touch), Phone (4a) Pro (13x13, AOD only)
- **App mode:** Requires system version 20250801+ for `setAppMatrixFrame`

### Shizuku

- **All devices:** Yes (if Shizuku app installed and running)

## Runtime Detection

Nothing Modes does NOT hardcode device assumptions. At runtime:

1. `CapabilityDetector` identifies device model via `Build.MODEL`, `Build.MANUFACTURER`
2. Nothing SDK `Common.isXXXXX()` confirms Nothing device and specific model
3. Android version checked via `Build.VERSION.SDK_INT`
4. Nothing OS version detected via system properties (investigation needed)
5. Permission status checked per-feature
6. Shizuku status checked via `ShizukuGateway`
7. Nothing SDK availability checked via class loading / service binding

All results stored in `DeviceCapabilities` — a snapshot used by `CapabilityManager` and `CapabilityResolver`.
